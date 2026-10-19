# HoloLingo

> 손끝에서 시작되는 언어의 세계. 다양한 언어를 직접 쓰고, 그 의미를 3D로 경험하는 체험형 언어 학습 플랫폼.

HoloLingo는 Android와 Jetpack Compose로 만드는 다국어 필기 학습 앱입니다. 사용자가 쓴 획을 ML Kit Digital Ink Recognition으로 인식하고, 인식 결과를 언어와 독립적인 개념에 연결한 뒤 같은 개념의 3D Scene을 보여줍니다.

## 모듈

```text
:app                 앱 진입점과 의존성 조립
:core:model          언어, 필기, 개념, Scene 공통 모델
:core:data           개념 저장소와 정답 판정
:core:recognition    ML Kit 필기 인식 어댑터
:core:renderer       3D/외부 출력 경계
:feature:practice    언어 선택, 필기, 판정, 시각화 UI
```

의존성은 기능에서 핵심 계층 방향으로만 흐릅니다.

```text
app → feature:practice → core:data / core:recognition / core:renderer
                                  ↓
                              core:model
```
