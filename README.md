# RelayPlugin - 마인크래프트 팀 릴레이 플러그인

마인크래프트 서버에서 팀 릴레이 게임을 즐길 수 있는 플러그인입니다. 플레이어들이 팀을 나누어 순차적으로 아이템을 수집하고 전달하는 협동 게임을 제공합니다.

## 📋 목차

- [서버 환경 및 Dependencies](#서버-환경-및-dependencies)
- [게임 구조](#게임-구조)
- [설정 파일](#설정-파일)
- [코드 구조](#코드-구조)
- [설치 및 사용법](#설치-및-사용법)
- [주요 기능](#주요-기능)
- [문제 해결](#문제-해결)
- [라이센스](#라이센스)
- [기여하기](#기여하기)
- [문의](#문의)

## 🖥️ 서버 환경 및 Dependencies

### 시스템 요구사항
- **Java**: 21 이상
- **마인크래프트 서버**: Paper 1.21.1 이상
- **Maven**: 3.6.0 이상 (빌드용)

### Dependencies
```xml
<dependency>
    <groupId>io.papermc.paper</groupId>
    <artifactId>paper-api</artifactId>
    <version>1.21.1-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

### 권한
플러그인 자체는 특별한 권한을 요구하지 않습니다. 모든 명령어는 기본적으로 모든 플레이어가 사용할 수 있습니다.

## 🎮 게임 구조

### 라운드 흐름
1. **팀 배정**: 플레이어들이 `/레드팀` 또는 `/블루팀` 명령어로 팀에 참가
2. **게임 시작**: 관리자가 `/게임시작` 명령어로 게임 시작
3. **순차 진행**: 각 팀의 첫 번째 주자가 첫 번째 목표 아이템 수집
4. **아이템 전달**: 목표 달성 시 다음 주자에게 인벤토리와 위치 전달
5. **목표 완료**: 모든 목표 아이템을 순서대로 수집한 팀이 승리

### 주요 명령어
| 명령어 | 설명 | 사용법 |
|--------|------|--------|
| `/레드팀` | 레드팀에 참가 | 모든 플레이어 |
| `/블루팀` | 블루팀에 참가 | 모든 플레이어 |
| `/게임시작` | 팀 릴레이 게임 시작 | 관리자 |
| `/게임종료` | 게임 강제 종료 | 관리자 |

### 게임 시스템
- **팀 관리**: 레드팀과 블루팀으로 구분
- **순서 시스템**: 팀 내 플레이어 순서대로 주자 역할 수행
- **아이템 전달**: 주자 변경 시 인벤토리와 위치 자동 교환
- **진행률 표시**: 보스바와 액션바로 실시간 진행 상황 표시
- **스코어보드**: 팀별 구성원과 순서 표시

## ⚙️ 설정 파일

### config.yml
```yaml
# 팀 릴레이: 아이템 목표(순서대로)
objectives:
  - DIAMOND_BLOCK
  - TINTED_GLASS
  - ENCHANTING_TABLE
  - NAME_TAG

# 액션바 업데이트 주기(틱). 10틱=0.5초
actionbarPeriodTicks: 10
```

### plugin.yml
```yaml
name: RelayPlugin
version: '1.0-SNAPSHOT'
main: yd.kingdom.relayPlugin.RelayPlugin
api-version: '1.21'
commands:
  레드팀:
    description: 레드팀으로 소속
    usage: /레드팀
  블루팀:
    description: 블루팀으로 소속
    usage: /블루팀
  게임시작:
    description: 팀 릴레이 게임 시작
    usage: /게임시작
  게임종료:
    description: 팀 릴레이 게임 종료
    usage: /게임종료
```

## 🏗️ 코드 구조

### 패키지 구조
```
yd.kingdom.relayPlugin/
├── RelayPlugin.java              # 메인 플러그인 클래스
├── command/                      # 명령어 처리
│   ├── RedTeamCommand.java      # 레드팀 참가 명령어
│   ├── BlueTeamCommand.java     # 블루팀 참가 명령어
│   ├── GameStartCommand.java    # 게임 시작 명령어
│   └── GameStopCommand.java     # 게임 종료 명령어
├── manager/                      # 핵심 관리 클래스
│   ├── GameManager.java         # 게임 진행 관리
│   └── TeamManager.java         # 팀 및 플레이어 관리
├── service/                      # 서비스 클래스
│   └── ScoreboardService.java   # 스코어보드 관리
├── listener/                     # 이벤트 리스너
│   └── ItemProgressListener.java # 아이템 진행 감지
└── util/                        # 유틸리티 클래스
    └── InventoryUtil.java       # 인벤토리 관련 유틸리티
```

### 주요 클래스 설명

#### RelayPlugin.java
- 플러그인의 메인 클래스
- 모든 매니저와 서비스 초기화
- 명령어와 리스너 등록

#### GameManager.java
- 게임의 전체적인 진행 관리
- 팀별 진행 단계 추적
- 아이템 전달 및 위치 교환 처리
- 보스바와 액션바 관리

#### TeamManager.java
- 팀별 플레이어 관리
- 팀 내 순서 관리
- 플레이어 팀 변경 처리

#### ScoreboardService.java
- 팀별 스코어보드 생성 및 관리
- 플레이어별 맞춤형 보드 표시

#### ItemProgressListener.java
- 아이템 획득 이벤트 감지
- 제작, 용광로 추출, 인벤토리 클릭 등 감지

## 📥 설치 및 사용법

### 1. 빌드
```bash
git clone https://github.com/your-username/RelayPlugin.git
cd RelayPlugin
mvn clean package
```

### 2. 설치
1. 생성된 `target/RelayPlugin-1.0-SNAPSHOT-shaded.jar` 파일을 서버의 `plugins` 폴더에 복사
2. 서버 재시작 또는 플러그인 리로드

### 3. 기본 사용법
1. **팀 배정**: 플레이어들이 `/레드팀` 또는 `/블루팀`으로 팀 참가
2. **게임 시작**: 관리자가 `/게임시작` 명령어 실행
3. **게임 진행**: 각 팀의 주자가 순서대로 목표 아이템 수집
4. **게임 종료**: 모든 목표 완료 시 자동 종료 또는 `/게임종료`로 강제 종료

### 4. 설정 커스터마이징
`config.yml` 파일에서 목표 아이템과 액션바 업데이트 주기를 조정할 수 있습니다.

## ✨ 주요 기능

### 🎯 자동 아이템 감지
- 아이템 획득, 제작, 용광로 추출 등 자동 감지
- 인벤토리 클릭 시에도 실시간 감지

### 🔄 자동 전달 시스템
- 주자 변경 시 인벤토리와 위치 자동 교환
- 아이템 손실 방지

### 📊 실시간 진행 상황
- 보스바로 현재 목표와 진행률 표시
- 액션바로 경과 시간 표시
- 스코어보드로 팀 구성원과 순서 표시

### 🛡️ 안전장치
- 현재 주자가 이미 목표 아이템을 가지고 있을 경우 자동 제거
- 다음 주자가 다음 목표 아이템을 미리 가지고 있을 경우 자동 제거

### 🎮 게임 모드 관리
- 주자는 서바이벌 모드
- 대기자는 관전자 모드
- 게임 종료 시 모든 플레이어 서바이벌 모드로 복원

## 🔧 문제 해결

### 일반적인 문제들

#### 게임이 시작되지 않음
- **원인**: 양 팀 모두 최소 1명 이상이어야 함
- **해결**: 각 팀에 플레이어가 참가했는지 확인

#### 아이템이 감지되지 않음
- **원인**: 플레이어가 현재 주자가 아님
- **해결**: 팀 순서 확인 및 올바른 주자 확인

#### 보스바가 표시되지 않음
- **원인**: 게임이 진행 중이지 않음
- **해결**: `/게임시작` 명령어로 게임 시작

#### 인벤토리 전달 문제
- **원인**: 다음 주자의 인벤토리가 가득 참
- **해결**: 다음 주자의 인벤토리 공간 확보

### 로그 확인
플러그인 로그는 서버 콘솔에서 확인할 수 있습니다:
```
[RelayPlugin] 야생 릴레이 플러그인 활성화
[RelayPlugin] 팀 릴레이 시작! 목표: DIAMOND_BLOCK → TINTED_GLASS → ENCHANTING_TABLE → NAME_TAG
```

## 📄 라이센스

이 프로젝트는 MIT 라이센스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 🤝 기여하기

프로젝트에 기여하고 싶으시다면:

1. 이 저장소를 포크합니다
2. 새로운 기능 브랜치를 생성합니다 (`git checkout -b feature/amazing-feature`)
3. 변경사항을 커밋합니다 (`git commit -m 'Add some amazing feature'`)
4. 브랜치에 푸시합니다 (`git push origin feature/amazing-feature`)
5. Pull Request를 생성합니다

### 개발 환경 설정
1. Java 21 설치
2. Maven 3.6.0+ 설치
3. IDE에서 프로젝트 열기
4. `mvn clean compile` 실행

### 코드 스타일
- Java 표준 네이밍 컨벤션 준수
- 적절한 주석 작성
- 예외 처리 포함

## 📞 문의

- **이슈 리포트**: [GitHub Issues](https://github.com/your-username/RelayPlugin/issues)
- **기능 요청**: [GitHub Discussions](https://github.com/your-username/RelayPlugin/discussions)
- **버그 리포트**: 상세한 재현 단계와 함께 이슈 생성

### 지원 정보
- **지원 버전**: Paper 1.21.1+
- **Java 버전**: 21+
- **최신 릴리즈**: [Releases](https://github.com/your-username/RelayPlugin/releases)

---

**RelayPlugin**으로 마인크래프트 서버에서 재미있는 팀 릴레이 게임을 즐겨보세요! 🎮✨