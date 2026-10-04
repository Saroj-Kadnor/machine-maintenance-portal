# Test Plan - Machine Maintenance Portal (Selenium UI tests)

## 1. Objective
Verify the five most critical user journeys of the portal through the real browser UI, using Selenium WebDriver (Java, JUnit 5), executed through Maven.

## 2. Scope and environment
| Item | Value |
|---|---|
| Application under test | Machine Maintenance Portal (Spring Boot, Thymeleaf, MySQL) |
| URL | `http://localhost:8081` (configurable with `-DbaseUrl=`) |
| Browser | Microsoft Edge (default); Chrome and Firefox supported with `-Dbrowser=` |
| Tools | Selenium 4, JUnit 5, Maven Surefire |
| Test code | `src/test/java/com/saroj/machine_maintenance_portal/selenium` |
| Test data | Seeded users (admin, manager, tech1) + records created by the tests with unique `SEL-<number>` codes |

## 3. Test data
| User | Password | Role |
|---|---|---|
| admin | admin123 | Admin |
| manager | manager123 | Manager |
| tech1 | tech123 | Technician |

Machine codes, problem texts and new technician usernames get a numeric suffix (current time) so every run uses fresh data.

## 4. Journeys and test cases
| ID | Journey | Steps | Expected result (assertion) |
|---|---|---|---|
| J1-TC1 | Login | Log in as admin | Dashboard heading shown; Machines menu visible |
| J1-TC2 | Login | Log in with a wrong password | URL contains `/login?error`; message "Invalid username or password." |
| J2-TC1 | Machines | Admin adds a machine | Success message "Machine added successfully"; machine in the list |
| J2-TC2 | Machines | Search by machine code | Exactly one row, containing that code |
| J2-TC3 | Machines | Search for a non-existing text | "No machines found." |
| J2-TC4 | Machines | Open Add Machine form | Date picker `max` attribute equals today (no future dates) |
| J3-TC1 | Maintenance | Manager adds a machine, creates a HIGH request for tech1, searches for it | Success message; row shows PENDING, HIGH and tech1 |
| J4-TC1 | Status workflow | Admin creates a job for tech1; tech1 clicks Start, then Complete | Status PENDING -> IN_PROGRESS -> COMPLETED |
| J4-TC2 | Status workflow | tech1 opens the pending job | Start button present; Cancel button absent |
| J5-TC1 | Role access | tech1 logs in, opens `/machines/add` | Limited menu; "Access denied" page |
| J5-TC2 | Role access | Manager, then Admin, open Technicians page | Add Technician button hidden for Manager, shown for Admin |
| J5-TC3 | Role access | Admin adds a technician; that technician logs in | Success message; dashboard opens for the new user |

## 5. Failure screenshot mechanism
`ScreenshotOnFailureExtension` (JUnit 5 extension) takes a PNG of the browser when any test fails, before the browser closes. Files are saved to `target/screenshots/FAILED_<class>_<test>_<time>.png`. Tests also save evidence screenshots (`EVIDENCE_...`) at key steps.

## 6. How to run
1. Start the application (Jenkins deployment on port 8081, or locally).
2. From the project folder:
   - `.\mvnw.cmd test -Pselenium`
   - Options: `-DbaseUrl=http://localhost:8080`, `-Dbrowser=chrome`, `-Dheadless=true`
3. Reports: console summary, `target/surefire-reports/` (txt and xml), screenshots in `target/screenshots/`.

Normal builds (`mvnw package`) skip these tests, so the Jenkins build does not need a running application.

## 7. Entry / exit criteria
- Entry: application running, MySQL running, demo users exist.
- Exit: all 12 test cases pass, or each failure is logged as an issue with its screenshot.
