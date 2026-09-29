# Security Alignment Reporting Guidelines and Template

When this skill is executed to apply security hardening updates to a codebase, the agent **MUST** generate a structured "Best Practices and Security Alignment Update" report for the developer. The report **must** be written to the session artifact folder (or printed in the final response) and include:

1. **Security alignment area:** The category of improvement applied (e.g., Safe Intent Redirection, Secure PendingIntent Configuration, ContentProvider Data Guarding).
2. **Impact and priority:** The potential safety risk addressed by the update (e.g., Component Hijacking Prevention, Private Data Isolation).
3. **Scope of changes:** A list of all modified classes, XML files, and dependencies.
4. **Implementation summary:** Concrete details of the solution (e.g., "Updated nested intent parsing to use the `IntentSanitizer` API with a strict component allowlist").
5. **Code diff:** Standard unified diffs showing the exact modifications.

## Best Practices and Security Alignment Update Template

Use the following markdown template when reporting changes to developers:

```markdown
### Best practices and security alignment update: [Security Alignment Area]

* **Improvement Description:** [Brief description of the hardening update and why it's recommended]
* **Priority Level:** [High / Medium / Low]
* **Alignment Action:** [Summary of updates, for example, converted to FLAG_IMMUTABLE]

#### Files modified
* `[Relative path to File 1]`
* `[Relative path to File 2]`

#### Implementation diff
```diff
// Insert Unified Diff here
```

#### Testing and verification
1. [Step 1 to verify the component behaves correctly, for example, run component unit test]
2. [Step 2 to verify regression safety]
```
