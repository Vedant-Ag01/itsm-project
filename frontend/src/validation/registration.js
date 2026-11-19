// Frontend policy. Align these limits with Vedant's contract before enabling registration.
export function validateRegistration(values) {
  const errors = {}
  const name = values.name.trim()
  const email = values.email.trim()
  if (!name) errors.name = 'Enter your name.'
  else if (name.length > 100) errors.name = 'Use 100 characters or fewer.'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 254) errors.email = 'Enter a valid email address.'
  if (values.password.length < 8) errors.password = 'Use at least 8 characters.'
  else if (values.password.length > 128) errors.password = 'Use 128 characters or fewer.'
  else if (!values.password.trim()) errors.password = 'Your password cannot contain only spaces.'
  if (values.confirmPassword !== values.password) errors.confirmPassword = 'Passwords do not match.'
  if (values.department.trim().length > 100) errors.department = 'Use 100 characters or fewer.'
  return errors
}

export function registrationPayload(values) {
  return {
    name: values.name.trim(),
    email: values.email.trim(),
    password: values.password,
    department: values.department.trim(),
  }
}
