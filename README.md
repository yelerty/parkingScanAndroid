# ParkWhere Scanner Android

iOS 버전과 동일한 기능을 제공하는 안드로이드 주차 스캐너 앱입니다.

## 🚀 주요 기능

### 📱 핵심 기능
- **자동 사진 스캔**: 갤러리의 최근 사진들을 자동으로 분석
- **OCR 텍스트 인식**: Google ML Kit을 사용한 한국어 텍스트 인식
- **주차 코드 검출**: B3-14, 1F-10, A1-05 등 다양한 주차장 코드 형식 지원
- **번호판 인식**: 한국 번호판 형식 자동 검출 및 사용자 번호판 매칭
- **GPS 위치 정보**: 사진의 위치 정보를 활용한 주차 위치 저장

### 🎯 스마트 필터링
- **스크린샷 제외**: 스크린샷 자동 필터링
- **GPS 필수**: 위치 정보가 없는 사진 제외
- **블랙리스트**: 원하지 않는 사진 영구 제외
- **중복 방지**: 동일한 사진 중복 처리 방지

### 🎨 사용자 인터페이스
- **Material Design 3**: 최신 Material Design 가이드라인 적용
- **iOS 앱과 동일한 색상**: 브랜드 일관성 유지
- **직관적인 네비게이션**: 쉬운 사용법
- **반응형 디자인**: 다양한 화면 크기 지원

## 🛠 기술 스택

### 📚 주요 라이브러리
- **Kotlin**: 100% Kotlin으로 개발
- **Architecture Components**: ViewModel, LiveData, Room
- **ML Kit**: Google ML Kit Text Recognition (Korean)
- **CameraX**: 카메라 기능
- **Room Database**: 로컬 데이터 저장
- **Glide**: 이미지 로딩 및 캐싱
- **Material Design**: UI 컴포넌트
- **Coroutines**: 비동기 처리

### 📁 프로젝트 구조
```
app/src/main/java/com/parkwhere/scanner/
├── data/                           # 데이터 모델 및 데이터베이스
│   ├── ParkingRecord.kt           # 주차 기록 데이터 모델
│   ├── ParkingDatabase.kt         # Room 데이터베이스
│   └── BlacklistItem.kt           # 블랙리스트 아이템
├── ui/                            # UI 관련 클래스
│   ├── MainActivity.kt            # 메인 액티비티
│   ├── viewmodels/
│   │   └── ParkingViewModel.kt    # 메인 뷰모델
│   └── adapters/
│       └── ParkingRecordAdapter.kt # RecyclerView 어댑터
└── utils/                         # 유틸리티 클래스
    ├── OCRProcessor.kt            # OCR 텍스트 인식
    ├── RegexDetector.kt           # 정규식 패턴 매칭
    └── ImageProcessor.kt          # 이미지 처리
```

## 🔧 설정 방법

### 1. 요구사항
- Android Studio Arctic Fox 이상
- Gradle 7.0 이상
- Android API 26 (Android 8.0) 이상

### 2. 프로젝트 빌드
```bash
cd ParkingScannerAndroid
./gradlew build
```

### 3. 에뮬레이터/디바이스에서 실행
```bash
./gradlew installDebug
```

## 📱 주요 화면

### 메인 화면
- 주차 기록 목록 표시
- Pull-to-refresh로 새로고침
- AdMob 배너 광고
- FAB를 통한 수동 스캔

### 설정 화면
- 사용자 번호판 등록
- 모든 기록 삭제
- 블랙리스트 관리

### 지도 화면
- 주차 위치를 지도에서 확인
- 외부 지도 앱 연동

## 🔍 핵심 알고리즘

### OCR 및 패턴 매칭
```kotlin
// 1. 주차장 시설 코드 우선 검색
val parkingCode = regexDetector.firstMatch(lines)
if (parkingCode != null && parkingCode.length <= 7) {
    detectedCode = parkingCode
}
// 2. 주차 코드가 없으면 번호판 검색
else {
    val plate = regexDetector.firstLicensePlateMatch(lines)
    if (plate != null && userPlate?.let { plate.endsWith(it) } == true) {
        detectedCode = plate
    }
}
```

### 지원하는 주차 코드 형식
- **기둥 번호**: B3-14, 1F-10, 2F-33, A1-05, C1-88
- **공백 구분**: 2F 27, B3 14, A1 05
- **붙어있는 형식**: B314, A105, C188, 1F10, 2F33
- **단순 형식**: B12, A5, C99
- **한글 포함**: 가123, 나456
- **지하 주차장**: 지하2층-15

## 📊 성능 최적화

### 이미지 처리 최적화
- **썸네일 우선 로드**: MediaStore 썸네일 활용
- **이미지 크기 조정**: 메모리 사용량 최적화
- **백그라운드 처리**: 메인 스레드 블록 방지

### 데이터베이스 최적화
- **Room 데이터베이스**: SQLite 최적화
- **인덱싱**: 빠른 검색을 위한 인덱스
- **배치 처리**: 대량 데이터 처리 최적화

## 🔐 권한

### 필수 권한
- `READ_EXTERNAL_STORAGE`: 갤러리 사진 접근
- `ACCESS_FINE_LOCATION`: GPS 위치 정보
- `ACCESS_COARSE_LOCATION`: 네트워크 위치 정보

### 선택 권한
- `CAMERA`: 카메라 기능 (향후 추가 예정)
- `INTERNET`: AdMob 광고

## 🎯 iOS 앱 대비 동등한 기능

| 기능 | iOS | Android | 상태 |
|------|-----|---------|------|
| 자동 사진 스캔 | ✅ | ✅ | 완료 |
| OCR 텍스트 인식 | ✅ | ✅ | 완료 |
| 정규식 패턴 매칭 | ✅ | ✅ | 완료 |
| GPS 위치 정보 | ✅ | ✅ | 완료 |
| 블랙리스트 기능 | ✅ | ✅ | 완료 |
| AdMob 광고 | ✅ | ✅ | 완료 |
| 설정 화면 | ✅ | ✅ | 완료 |
| 지도 연동 | ✅ | 🔄 | 진행중 |

## 🚀 향후 계획

### Phase 1 (완료)
- ✅ 기본 프로젝트 구조
- ✅ 데이터 모델 및 데이터베이스
- ✅ OCR 및 정규식 검출
- ✅ 메인 UI 구현

### Phase 2 (예정)
- 🔄 지도 화면 완성
- 🔄 설정 화면 구현
- 🔄 테스트 및 버그 수정
- 🔄 성능 최적화

### Phase 3 (예정)
- 📋 앱 아이콘 및 스플래시 화면
- 📋 다국어 지원 (한국어/영어)
- 📋 Google Play 스토어 배포
- 📋 사용자 피드백 반영

## 📄 라이선스

이 프로젝트는 iOS 버전과 동일한 라이선스를 따릅니다.