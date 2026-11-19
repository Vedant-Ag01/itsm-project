import assert from 'node:assert/strict'
import test from 'node:test'
import { validateRegistration, registrationPayload } from '../src/validation/registration.js'
const valid = { name: ' Naman Yerra ', email: ' naman@bits.edu ', password: 'a secure password', confirmPassword: 'a secure password', department: ' CSE ' }
test('accept valid values', () => assert.deepEqual(validateRegistration(valid), {}))
test('blank name rejected', () => assert.ok(validateRegistration({ ...valid, name: '   ' }).name))
test('name length boundary', () => {
  assert.equal(validateRegistration({ ...valid, name: 'a'.repeat(100) }).name, undefined)
  assert.ok(validateRegistration({ ...valid, name: 'a'.repeat(101) }).name)
})
test('malformed email rejected', () => {
  for (const email of ['', 'naman', 'a@', 'a@@bits.edu', 'a b@bits.edu']) assert.ok(validateRegistration({ ...valid, email }).email)
})
test('password below eight rejected', () => assert.ok(validateRegistration({ ...valid, password: '1234567', confirmPassword: '1234567' }).password))
test('eight and 128 character boundaries accepted', () => {
  for (const length of [8, 128]) assert.deepEqual(validateRegistration({ ...valid, password: 'x'.repeat(length), confirmPassword: 'x'.repeat(length) }), {})
})
test('129 character password rejected', () => assert.ok(validateRegistration({ ...valid, password: 'x'.repeat(129) }).password))
test('whitespace-only password rejected', () => assert.ok(validateRegistration({ ...valid, password: '        ', confirmPassword: '        ' }).password))
test('mismatch rejected', () => assert.ok(validateRegistration({ ...valid, confirmPassword: 'different' }).confirmPassword))
test('department optional', () => assert.deepEqual(validateRegistration({ ...valid, department: '' }), {}))
test('department over limit rejected', () => assert.ok(validateRegistration({ ...valid, department: 'x'.repeat(101) }).department))
test('payload whitelists fields and trims non-secret text', () => {
  assert.deepEqual(registrationPayload({ ...valid, role: 'ADMIN', userId: 'A1' }), { name: 'Naman Yerra', email: 'naman@bits.edu', password: 'a secure password', department: 'CSE' })
})
test('password spaces preserved', () => assert.equal(registrationPayload({ ...valid, password: ' secret pass ' }).password, ' secret pass '))
