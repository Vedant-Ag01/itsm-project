package edu.bits.itsm.api;

import edu.bits.itsm.assignment.AssignmentStrategy;
import edu.bits.itsm.auth.AuthInterceptor;
import edu.bits.itsm.auth.AuthService;
import edu.bits.itsm.auth.PasswordHasher;
import edu.bits.itsm.classification.KeywordCategoryClassifier;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.auth.UserAccount;
import edu.bits.itsm.auth.UserAccountTable;
import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.ServiceInfo;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.Urgency;
import edu.bits.itsm.duplicate.DuplicateDetector;
import edu.bits.itsm.duplicate.DuplicateMatch;
import edu.bits.itsm.event.IncidentObserver;
import edu.bits.itsm.priority.PriorityStrategy;
import edu.bits.itsm.repository.InMemoryIncidentRepository;
import edu.bits.itsm.repository.IncidentRepository;
import edu.bits.itsm.repository.ServiceRepository;
import edu.bits.itsm.search.IncidentSearchService;
import edu.bits.itsm.service.IncidentFactory;
import edu.bits.itsm.service.IncidentService;
import edu.bits.itsm.service.ServiceCatalog;
import edu.bits.itsm.sla.DefaultSlaCalculator;
import edu.bits.itsm.sla.DefaultSlaPolicy;
import edu.bits.itsm.sla.HeuristicSlaRiskPredictor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tests the real HTTP layer: login checks, roles, and the status codes errors turn into.
// No database: users, services and incidents are kept in memory.
class ApiBehaviourTest {

    // ---------- small stand-ins ----------

    private static class FixedPriority implements PriorityStrategy {
        public Priority calculate(Impact impact, Urgency urgency,
                                  BusinessCriticality criticality, int affectedUsers) {
            return Priority.P2;
        }

        public String getName() {
            return "Fixed";
        }
    }

    private static class FirstTechnician implements AssignmentStrategy {
        public Optional<Technician> selectTechnician(Incident incident,
                                                     List<Technician> candidates,
                                                     Map<Technician, Integer> activeLoad) {
            return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0));
        }

        public String getName() {
            return "First";
        }
    }

    private static class NoDuplicates implements DuplicateDetector {
        public List<DuplicateMatch> findSimilar(Incident incoming, List<Incident> existing) {
            return new ArrayList<>();
        }
    }

    private static class InMemoryServices implements ServiceRepository {
        private final List<ServiceInfo> items = new ArrayList<>();

        public List<ServiceInfo> findAll() {
            return new ArrayList<>(items);
        }

        public List<ServiceInfo> findActive() {
            List<ServiceInfo> active = new ArrayList<>();
            for (ServiceInfo s : items) {
                if (s.active()) {
                    active.add(s);
                }
            }
            return active;
        }

        public Optional<ServiceInfo> findByName(String name) {
            for (ServiceInfo s : items) {
                if (name != null && s.name().equalsIgnoreCase(name.trim())) {
                    return Optional.of(s);
                }
            }
            return Optional.empty();
        }

        public Optional<ServiceInfo> findById(long id) {
            for (ServiceInfo s : items) {
                if (s.id() != null && s.id() == id) {
                    return Optional.of(s);
                }
            }
            return Optional.empty();
        }

        public ServiceInfo save(ServiceInfo service) {
            ServiceInfo saved = service;
            if (service.id() == null) {
                saved = new ServiceInfo((long) items.size() + 1, service.name(),
                        service.description(), service.criticality(), service.active());
            } else {
                items.removeIf(s -> s.id().equals(service.id()));
            }
            items.add(saved);
            return saved;
        }
    }

    // A UserAccountTable that keeps users in a map (only the methods the app uses)
    private static UserAccountTable fakeUsers(Map<String, UserAccount> byId) {
        return (UserAccountTable) Proxy.newProxyInstance(
                UserAccountTable.class.getClassLoader(),
                new Class<?>[]{UserAccountTable.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "findById":
                            return Optional.ofNullable(byId.get((String) args[0]));
                        case "findByEmail":
                            for (UserAccount u : byId.values()) {
                                if (u.email.equals(args[0])) {
                                    return Optional.of(u);
                                }
                            }
                            return Optional.empty();
                        case "save":
                            return args[0];
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "fakeUsers";
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private static UserAccount account(String id, String name, String email, Role role) {
        UserAccount a = new UserAccount();
        a.id = id;
        a.name = name;
        a.email = email;
        a.department = "IT";
        a.role = role;
        a.passwordSalt = PasswordHasher.newSalt();
        a.passwordHash = PasswordHasher.hash("password123", a.passwordSalt);
        return a;
    }

    // ---------- setup ----------

    private MockMvc mvc;
    private String asha;       // employee who reports
    private String nikhil;     // a different employee
    private String ravi;       // technician (assigned to things)
    private String meera;      // technician (never assigned)
    private String admin;

    private static final String INCIDENT_JSON = """
            {"title":"WiFi down in library","description":"No WiFi on the second floor",
             "serviceName":"Campus WiFi","criticality":"HIGH","impact":"HIGH","urgency":"HIGH",
             "affectedUsers":30,"category":"NETWORK"}""";

    @BeforeEach
    void setUp() {
        Map<String, UserAccount> users = new HashMap<>();
        users.put("E1", account("E1", "Asha", "asha@bits.edu", Role.EMPLOYEE));
        users.put("E2", account("E2", "Nikhil", "nikhil@bits.edu", Role.EMPLOYEE));
        users.put("T1", account("T1", "Ravi", "ravi@bits.edu", Role.TECHNICIAN));
        users.put("T2", account("T2", "Meera", "meera@bits.edu", Role.TECHNICIAN));
        users.put("A1", account("A1", "Admin", "admin@bits.edu", Role.ADMIN));
        AuthService auth = new AuthService(fakeUsers(users));
        asha = token(auth, "asha@bits.edu");
        nikhil = token(auth, "nikhil@bits.edu");
        ravi = token(auth, "ravi@bits.edu");
        meera = token(auth, "meera@bits.edu");
        admin = token(auth, "admin@bits.edu");

        IncidentRepository repository = new InMemoryIncidentRepository();
        List<Technician> technicians = new ArrayList<>();
        technicians.add(new Technician("T1", "Ravi", "ravi@bits.edu"));
        technicians.add(new Technician("T2", "Meera", "meera@bits.edu"));
        IncidentService incidents = new IncidentService(repository, new IncidentFactory(repository),
                new FixedPriority(), new FirstTechnician(), new NoDuplicates(),
                new ArrayList<IncidentObserver>(), technicians);

        ServiceCatalog catalog = new ServiceCatalog(new InMemoryServices());
        catalog.create("Campus WiFi", "Wireless network", BusinessCriticality.HIGH);

        IncidentController incidentController = new IncidentController(incidents,
                new IncidentSearchService(), new DefaultSlaCalculator(new DefaultSlaPolicy()),
                catalog, new KeywordCategoryClassifier(KeywordCategoryClassifier.defaultRules()));

        mvc = MockMvcBuilders
                .standaloneSetup(incidentController, new IncidentNoteController(incidents),
                        new ServiceController(catalog), new WorkloadController(incidents, technicians),
                        new ClassificationController(
                                new KeywordCategoryClassifier(KeywordCategoryClassifier.defaultRules())),
                        new SlaRiskController(incidents, new HeuristicSlaRiskPredictor(
                                new DefaultSlaCalculator(new DefaultSlaPolicy()))))
                .addInterceptors(new AuthInterceptor(auth))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static String token(AuthService auth, String email) {
        return auth.login(email, "password123").token();
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private int code(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    // Reports an incident as asha and returns its id
    private String reportIncident() throws Exception {
        MvcResult result = mvc.perform(as(json(post("/api/incidents"), INCIDENT_JSON), asha))
                .andExpect(status().isCreated()).andReturn();
        Matcher matcher = Pattern.compile("\"id\":\"(INC\\d+)\"")
                .matcher(result.getResponse().getContentAsString());
        assertTrue(matcher.find(), "response should contain the incident id");
        return matcher.group(1);
    }

    // ---------- 401: not logged in ----------

    @Test
    void noTokenGives401() throws Exception {
        assertEquals(401, code(get("/api/services")));
    }

    @Test
    void aMadeUpTokenGives401() throws Exception {
        assertEquals(401, code(as(get("/api/services"), "not-a-real-token")));
    }

    // ---------- 403: wrong role or not yours ----------

    @Test
    void anEmployeeCannotUseAnAdminEndpoint() throws Exception {
        assertEquals(403, code(as(get("/api/admin/services"), asha)));
    }

    @Test
    void anEmployeeCannotCloseAnIncident() throws Exception {
        String id = reportIncident();
        assertEquals(403, code(as(json(post("/api/incidents/" + id + "/close"),
                "{\"note\":\"done\"}"), asha)));
    }

    @Test
    void anotherEmployeeCannotSeeSomeoneElsesIncident() throws Exception {
        String id = reportIncident();
        assertEquals(403, code(as(get("/api/incidents/" + id), nikhil)));
    }

    @Test
    void anotherEmployeeCannotEditIt() throws Exception {
        String id = reportIncident();
        assertEquals(403, code(as(json(put("/api/incidents/" + id), "{\"title\":\"Hijacked\"}"), nikhil)));
    }

    @Test
    void aTechnicianNotAssignedCannotResolve() throws Exception {
        String id = reportIncident();
        assertEquals(403, code(as(json(post("/api/incidents/" + id + "/resolve"),
                "{\"note\":\"fixed\"}"), meera)));
    }

    // ---------- 404 ----------

    @Test
    void anUnknownIncidentGives404() throws Exception {
        assertEquals(404, code(as(get("/api/incidents/INC9999"), admin)));
    }

    // ---------- 400: bad input ----------

    @Test
    void anUnknownServiceGives400() throws Exception {
        String body = INCIDENT_JSON.replace("Campus WiFi", "Nonexistent Thing");
        assertEquals(400, code(as(json(post("/api/incidents"), body), asha)));
    }

    @Test
    void anEmptyTitleGives400() throws Exception {
        String body = INCIDENT_JSON.replace("WiFi down in library", "   ");
        assertEquals(400, code(as(json(post("/api/incidents"), body), asha)));
    }

    @Test
    void brokenJsonGives400() throws Exception {
        assertEquals(400, code(as(json(post("/api/incidents"), "{ this is not json"), asha)));
    }

    @Test
    void anInvalidEnumValueGives400() throws Exception {
        String body = INCIDENT_JSON.replace("\"impact\":\"HIGH\"", "\"impact\":\"HUGE\"");
        assertEquals(400, code(as(json(post("/api/incidents"), body), asha)));
    }

    @Test
    void aTooLongTitleGives400() throws Exception {
        String body = INCIDENT_JSON.replace("WiFi down in library", "x".repeat(300));
        assertEquals(400, code(as(json(post("/api/incidents"), body), asha)));
    }

    @Test
    void aSillyAffectedUsersNumberGives400() throws Exception {
        String body = INCIDENT_JSON.replace("\"affectedUsers\":30", "\"affectedUsers\":99999999");
        assertEquals(400, code(as(json(post("/api/incidents"), body), asha)));
    }

    @Test
    void editingWithAnEmptyTitleGives400() throws Exception {
        String id = reportIncident();
        assertEquals(400, code(as(json(put("/api/incidents/" + id), "{\"title\":\"  \"}"), asha)));
    }

    // ---------- 409: not allowed in the current state ----------

    @Test
    void closingAnIncidentThatIsNotResolvedGives409() throws Exception {
        String id = reportIncident();
        assertEquals(409, code(as(json(post("/api/incidents/" + id + "/close"),
                "{\"note\":\"done\"}"), admin)));
    }

    @Test
    void verifyingBeforeItIsResolvedGives409() throws Exception {
        String id = reportIncident();
        assertEquals(200, code(as(post("/api/incidents/" + id + "/auto-assign"), admin)));
        assertEquals(409, code(as(post("/api/incidents/" + id + "/verify"), asha)));
    }

    @Test
    void editingAResolvedIncidentGives409() throws Exception {
        String id = reportIncident();
        assertEquals(200, code(as(post("/api/incidents/" + id + "/auto-assign"), admin)));
        assertEquals(200, code(as(post("/api/incidents/" + id + "/acknowledge"), ravi)));
        assertEquals(200, code(as(post("/api/incidents/" + id + "/start-work"), ravi)));
        assertEquals(200, code(as(json(post("/api/incidents/" + id + "/resolve"),
                "{\"note\":\"Restarted the access point\"}"), ravi)));
        assertEquals(409, code(as(json(put("/api/incidents/" + id), "{\"title\":\"Too late\"}"), asha)));
    }

    // ---------- happy paths ----------

    @Test
    void reportedIncidentsCanBeEditedByTheReporter() throws Exception {
        String id = reportIncident();
        MvcResult result = mvc.perform(as(json(put("/api/incidents/" + id),
                "{\"title\":\"WiFi down on all floors\",\"affectedUsers\":200}"), asha))
                .andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("WiFi down on all floors"));
        assertTrue(body.contains("200"));
    }

    @Test
    void adminCanAddAServiceButNotTwiceWithTheSameName() throws Exception {
        String body = "{\"name\":\"Library Kiosk\",\"description\":\"Kiosks\",\"criticality\":\"HIGH\"}";
        assertEquals(201, code(as(json(post("/api/admin/services"), body), admin)));
        assertEquals(400, code(as(json(post("/api/admin/services"), body), admin)));
    }

    @Test
    void anUnknownCriticalityGives400() throws Exception {
        String body = "{\"name\":\"X\",\"description\":\"d\",\"criticality\":\"SUPER\"}";
        assertEquals(400, code(as(json(post("/api/admin/services"), body), admin)));
    }

    // ---------- asking the reporter for more information ----------

    @Test
    void theAssignedTechnicianCanAskAndTheReporterCanAnswer() throws Exception {
        String id = reportIncident();
        assertEquals(200, code(as(post("/api/incidents/" + id + "/auto-assign"), admin)));

        String asked = mvc.perform(as(json(post("/api/incidents/" + id + "/request-info"),
                "{\"message\":\"Which floor exactly?\"}"), ravi))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(asked.contains("\"awaitingInfo\":true"));

        String answered = mvc.perform(as(json(post("/api/incidents/" + id + "/provide-info"),
                "{\"message\":\"Second floor, east wing\"}"), asha))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(answered.contains("\"awaitingInfo\":false"));
    }

    @Test
    void aTechnicianNotAssignedCannotRequestInfo() throws Exception {
        String id = reportIncident();
        assertEquals(200, code(as(post("/api/incidents/" + id + "/auto-assign"), admin)));
        assertEquals(403, code(as(json(post("/api/incidents/" + id + "/request-info"),
                "{\"message\":\"Which floor?\"}"), meera)));
    }

    @Test
    void anotherEmployeeCannotAnswerForTheReporter() throws Exception {
        String id = reportIncident();
        assertEquals(200, code(as(post("/api/incidents/" + id + "/auto-assign"), admin)));
        assertEquals(200, code(as(json(post("/api/incidents/" + id + "/request-info"),
                "{\"message\":\"Which floor?\"}"), ravi)));
        assertEquals(403, code(as(json(post("/api/incidents/" + id + "/provide-info"),
                "{\"message\":\"Not my incident\"}"), nikhil)));
    }

    // ---------- workload ----------

    @Test
    void aTechnicianSeesTheirOwnWorkload() throws Exception {
        String id = reportIncident();
        assertEquals(200, code(as(post("/api/incidents/" + id + "/auto-assign"), admin)));
        String body = mvc.perform(as(get("/api/technician/workload"), ravi))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("\"activeIncidents\":1"));
        assertTrue(body.contains(id));
    }

    @Test
    void anEmployeeCannotSeeTheTechnicianWorkloadScreen() throws Exception {
        assertEquals(403, code(as(get("/api/technician/workload"), asha)));
    }

    @Test
    void whenTheLimitIsReachedAutoAssignGives409() throws Exception {
        assertEquals(200, code(as(json(put("/api/admin/workload-limit"),
                "{\"maxActiveIncidents\":1}"), admin)));
        // two technicians exist but the test strategy always picks the first one,
        // and only Ravi is "first": once he is full, nobody else is offered
        String first = reportIncident();
        String second = reportIncident();
        assertEquals(200, code(as(post("/api/incidents/" + first + "/auto-assign"), admin)));
        // Meera is still free, so the second goes to her; the third finds nobody
        assertEquals(200, code(as(post("/api/incidents/" + second + "/auto-assign"), admin)));
        String third = reportIncident();
        assertEquals(409, code(as(post("/api/incidents/" + third + "/auto-assign"), admin)));
    }

    @Test
    void aWorkloadLimitBelowOneGives400() throws Exception {
        assertEquals(400, code(as(json(put("/api/admin/workload-limit"),
                "{\"maxActiveIncidents\":0}"), admin)));
    }

    // ---------- automatic category ----------

    @Test
    void aMissingCategoryIsWorkedOutFromTheText() throws Exception {
        String body = INCIDENT_JSON.replace(",\"category\":\"NETWORK\"", "");
        String response = mvc.perform(as(json(post("/api/incidents"), body), asha))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertTrue(response.contains("\"category\":\"NETWORK\""));
    }

    @Test
    void aMissingCategoryWithNoClueInTheTextGives400() throws Exception {
        String body = INCIDENT_JSON.replace(",\"category\":\"NETWORK\"", "")
                .replace("WiFi down in library", "zzz qqq")
                .replace("No WiFi on the second floor", "xxx yyy");
        assertEquals(400, code(as(json(post("/api/incidents"), body), asha)));
    }

    @Test
    void theSuggestEndpointNamesACategory() throws Exception {
        String response = mvc.perform(as(json(post("/api/classification/suggest"),
                "{\"title\":\"VPN will not connect\",\"description\":\"Cannot reach internal systems\"}"), asha))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(response.contains("NETWORK"));
    }

    @Test
    void theSuggestEndpointNeedsALogin() throws Exception {
        assertEquals(401, code(json(post("/api/classification/suggest"),
                "{\"title\":\"VPN\",\"description\":\"x\"}")));
    }

    // ---------- SLA risk ----------

    @Test
    void theReporterCanSeeTheSlaRiskOfTheirIncident() throws Exception {
        String id = reportIncident();
        String body = mvc.perform(as(get("/api/incidents/" + id + "/sla-risk"), asha))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("\"level\""));
        assertTrue(body.contains("\"reason\""));
    }

    @Test
    void anotherEmployeeCannotSeeTheSlaRiskOfSomeoneElsesIncident() throws Exception {
        String id = reportIncident();
        assertEquals(403, code(as(get("/api/incidents/" + id + "/sla-risk"), nikhil)));
    }

    @Test
    void theAdminSeesAListOfOpenIncidentsWithRisk() throws Exception {
        String id = reportIncident();
        String body = mvc.perform(as(get("/api/admin/sla-risks"), admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(body.contains(id));
    }

    @Test
    void anEmployeeCannotUseTheAdminRiskList() throws Exception {
        assertEquals(403, code(as(get("/api/admin/sla-risks"), asha)));
    }

    @Test
    void anUnknownMinLevelGives400() throws Exception {
        assertEquals(400, code(as(get("/api/admin/sla-risks?minLevel=EXTREME"), admin)));
    }
}
