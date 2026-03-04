# ADR 0008: Letter Management Strategy

## Status
Accepted

## Context
The requirement states: "Letter management should be as painless as possible for the business."

Debt collection involves numerous letter types:
- Debt notifications
- Payment reminders
- Wage garnishment notices (to debtor and employer)
- Offsetting notifications
- Payment confirmations
- Appeal acknowledgements

Challenges:
- Legal requirements for letter content
- Multiple delivery channels (Digital Post, e-Boks, physical mail)
- Versioning of templates
- Multi-language support (Danish + potentially English)
- Business user modifications without developer involvement
- Audit trail for sent letters

## Decision
We implement a template-based letter management system:

### Architecture
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Letter Service │────▶│ Template Engine │────▶│ Delivery Service│
│                 │     │   (Freemarker)  │     │  (Digital Post) │
└─────────────────┘     └─────────────────┘     └─────────────────┘
         │
         ▼
┌─────────────────┐
│ Template Store  │
│ (Versioned)     │
└─────────────────┘
```

### Template Management
1. **Template Format**: Freemarker (`.ftl`) for flexibility
2. **Storage**: Database-backed with version history
3. **Editing**: Web-based template editor for business users
4. **Variables**: Standard variable set per letter type
5. **Preview**: Preview with sample data before publishing
6. **Approval**: Two-person approval for production templates

### Template Structure
```
templates/
├── debt-notification/
│   ├── v1.0.0/
│   │   ├── template.ftl
│   │   ├── metadata.json
│   │   └── sample-data.json
│   └── v1.1.0/
├── payment-reminder/
└── ...
```

### Delivery Channels
| Channel | Primary Use | Integration |
|---------|------------|-------------|
| Digital Post | Citizens with Digital Post | MitID/NemID lookup |
| e-Boks | Alternative digital | e-Boks API |
| Physical mail | Fallback | Print provider API |

### Business Features
- **Bulk sending**: Schedule batch letter runs
- **A/B testing**: Test letter effectiveness
- **Analytics**: Open rates, response rates
- **Localization**: i18n support
- **Attachments**: PDF generation, supporting documents

## Consequences

### Positive
- Business users can modify letters without code changes
- Version control for legal compliance
- Consistent formatting across channels
- Audit trail for all communications

### Negative
- Template engine learning curve
- Complex approval workflow
- Multiple delivery channel integrations

### Mitigations
- User-friendly template editor
- Training for business users
- Fallback mechanisms for delivery failures
- Comprehensive testing before deployment
