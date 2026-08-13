# AGENTS.md

This file is the entry point for AI agents.

Before any task:
1. Read this file.
2. Read `rules/README.md`.
3. Read the specific rule file relevant to the task.
4. Read relevant module docs/source.
5. Do not guess.

For Git, Flyway, validation, integration, and environment/config setup rules, follow the files in `rules/`.
For environment/config setup, read `rules/ENV_CONFIG_RULES.md`.
Do not commit secrets or local profile files.

## Global AI Rules

- Save tokens.
- Search before reading large files.
- Prefer `rg` for exact names, routes, classes, methods, errors, and env keys.
- Use semantic search only when the feature location or behavior flow is unclear.
- Use MCP only when it is useful, and ask before any MCP write action.
- Do not scan the whole repo unless necessary.
- Patch minimally.
- Keep existing style.
- Do not touch unrelated modules.
- Do not touch production config, secrets, DB, Docker, deploy files, or protected files without confirmation.
- Do not run build, dev, test, deploy, Maven, Docker, reset, clean, or destructive commands without confirmation.
- Do not modify Java source, migrations, application config, `.env`, or feature code during docs-only rules tasks.
- Preserve Vietnamese UTF-8. Never create mojibake or replacement characters.

## Required Reading

- `rules/README.md`
- `rules/AI_PROJECT_RULES.md`
- `rules/GIT_WORKFLOW_RULES.md`
- `rules/ENV_CONFIG_RULES.md`
- `rules/DB_MIGRATION_POLICY.md`
- `rules/VALIDATION_CHECKLIST.md`
- `rules/SPRINT_INTEGRATION_CHECKLIST.md` when preparing merges or integration branches

Module docs such as `docs/inbound-workflow.md` and task READMEs provide business and implementation context. The `rules/` folder contains operating rules.

## Custom Workflow Rules (BẮT BUỘC)

Người dùng đã thiết lập Quy trình làm việc tĩnh (Strict Skills Harness). Đối với MỌI Task code, AI phải tuân thủ tuyệt đối quy trình sau:
1. **Lệnh /grill-me (Brainstorm Spec):** Luôn dùng skill `@brainstorming` (tạo câu hỏi trắc nghiệm MCQs) để chốt edge cases với người dùng. **Tuyệt đối CẤM viết code** khi chưa có spec được chốt.
2. **Khai báo chuẩn Core Protocol:** Mọi câu trả lời code phải luôn bắt đầu bằng `🤖 Applying knowledge of @[agent]...` và `📚 Using skill: @[skill]...`.
3. **Áp dụng `@ponytail`:** Luôn dùng skill `@ponytail` mức độ `full` (viết code ngắn gọn, không đẻ thêm class/DTO thừa nếu framework đã có sẵn).
4. **Code Comments & Docs:** 
   - Chỉ thêm comment ngắn khi thật sự cần; không comment dài dòng hoặc giải thích lại code.
   - Tạo 1 file `docs/README_T[Mã Task].md` tóm tắt API (Chức năng, Logic, JSON Request/Response).
5. **Verify:** Bắt buộc chạy lệnh `mvnw clean compile` thành công (BUILD SUCCESS) thì mới được phép cập nhật file `progress.md` và `feature_list.json`.
