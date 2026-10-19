# Git 컨벤션

HoloLingo는 변경 이력을 쉽게 이해하고 안전하게 병합할 수 있도록 아래 규칙을 사용합니다.

## 브랜치

- `main`: 항상 배포 가능한 상태를 유지합니다.
- 기능 브랜치는 최신 `main`에서 생성합니다.
- 브랜치 이름은 `<type>/<짧은-kebab-case-설명>` 형식을 사용합니다.

예시:

```text
feat/speech-practice
fix/camera-permission
docs/setup-guide
```

허용하는 `type`은 아래 커밋 타입과 같습니다.

## 커밋

[Conventional Commits](https://www.conventionalcommits.org/) 형식을 사용합니다.

```text
<type>(<scope>): <subject>
```

- `type`과 `scope`는 영문 소문자로 작성합니다.
- `scope`는 선택 사항이며 변경 영역을 짧게 표시합니다.
- `subject`는 명령형으로 간결하게 작성하고 마침표를 붙이지 않습니다.
- 한 커밋에는 하나의 논리적인 변경만 담습니다.

| 타입 | 용도 |
| --- | --- |
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서 변경 |
| `style` | 동작에 영향 없는 서식 변경 |
| `refactor` | 기능 변화 없는 코드 구조 개선 |
| `test` | 테스트 추가 또는 수정 |
| `build` | 빌드 시스템 또는 의존성 변경 |
| `ci` | CI 설정 변경 |
| `chore` | 그 밖의 유지보수 작업 |
| `revert` | 이전 변경 되돌리기 |

예시:

```text
feat(practice): add pronunciation feedback
fix(camera): handle denied permission
docs: add Git contribution guide
```

호환성을 깨는 변경은 타입 뒤에 `!`를 붙이고 본문에 `BREAKING CHANGE:`를 작성합니다.

```text
feat(api)!: change lesson response format

BREAKING CHANGE: lesson items now use an id-based structure.
```

## Pull Request

1. `main`에서 작업 브랜치를 생성합니다.
2. 커밋 전 `./gradlew test`를 실행합니다.
3. PR 제목도 커밋 형식을 따릅니다.
4. PR 본문에 변경 내용, 테스트 결과, 관련 이슈를 작성합니다.
5. 리뷰 반영 후 squash merge를 권장합니다.
