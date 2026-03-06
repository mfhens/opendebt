# Petition 002 Outcome Contract

## Acceptance criteria

1. OpenDebt supports creation of a new fordring via the API for a fordringshaver.
2. An API request to create a fordring is accepted for processing only when it uses a valid OCES3 certificate.
3. An API request to create a fordring without a valid OCES3 certificate is rejected and does not create a debt post.
4. OpenDebt supports manual creation of a new fordring via the fordringshaverportal for eligible fordringshavere.
5. A portal user can create a fordring only when logged in with MitID Erhverv.
6. A portal user can create a fordring only for the fordringshaver linked to that user’s MitID Erhverv identity.
7. Every submitted fordring is evaluated for inddrivelsesparathed before any debt post is created.
8. If the fordring is not inddrivelsesparat, OpenDebt rejects the creation, returns an error message stating the reason, and does not create a debt post.
9. If the fordring is inddrivelsesparat, OpenDebt creates a new debt post for the relevant person or company.
10. When a new debt post is created from an inddrivelsesparat fordring, bookkeeping is updated.

## Definition of done

- The requirements distinguish clearly between API-based creation and portal-based manual creation.
- Channel-specific authentication requirements are testable.
- Portal access restriction to the linked fordringshaver is testable.
- The inddrivelsesparathed evaluation is defined as a mandatory gate before debt creation.
- The rejection path for non-inddrivelsesparat fordringer is testable, including return of the reason.
- The successful creation path is testable, including debt post creation and bookkeeping update.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- A fordring can be created via API without a valid OCES3 certificate.
- A portal user can create a fordring without MitID Erhverv login.
- A portal user can create a fordring for another fordringshaver than the one linked to the user’s MitID Erhverv identity.
- A debt post is created before the fordring is evaluated for inddrivelsesparathed.
- A non-inddrivelsesparat fordring is created as a debt post.
- A non-inddrivelsesparat fordring is rejected without stating the reason.
- A successful, inddrivelsesparat creation does not update bookkeeping.
