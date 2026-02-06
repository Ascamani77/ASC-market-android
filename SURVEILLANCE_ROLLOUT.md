Surveillance-First Rollout Plan

Goal:
- Promote Macro Intelligence Stream as the default user surface (≈90% UPCOMING visuals).

Metrics to monitor:
- % sessions landing on MacroStream (sessionLandingCount)
- Clicks to execution (clicksToExecutionCount)
- User overrides / opt-ins (userOverrideCount)
- Ingestion drop rate (ingestionDroppedCount)
- False positives/negatives for vigilance nodes (requires offline labeling)
- Micro-jitter / clock-drift KPIs

Phased rollout:
1. Internal/dev builds: enable `promoteMacroStream` default in debug builds; monitor metrics for 3 days.
2. Beta / staged users: enable flag via remote config for 5-10% of beta; collect metrics and user feedback.
3. Wider rollout: 25% → 50% → 100% while watching override rates and error rates.
4. Post-rollout: bake changes into stable channels and provide documentation/training for support.

Safety & rollback:
- `promoteMacroStream` is persisted and reversible — use it to quickly roll back.
- Require opt-in modal before enabling execution surfaces; audit all opt-ins.
- Maintain a simple health dashboard showing the metrics listed above.

QA checklist before each phase:
- Full `./gradlew clean build` passes.
- Automated smoke tests for navigation and opt-in flow.
- Manual verification that MacroStream shows 90/10 ratio on common devices.
- Security review: ensure no API keys are accepted/persisted inside UI paste flows.

Contact:
- Engineering oncall: eng-oncall@example.com
- Product owner: product@example.com
