# 🎬 MovieInfoBot

Discord에서 사용자의 영화 관련 질문을 받아 **TMDB 영화 정보와 RAG(Vector Store), Ollama를 활용하여 답변하는 AI 영화 정보 봇**입니다.

사용자가 Discord에서 `!영화` 명령어를 통해 영화 제목이나 영화와 관련된 질문을 입력하면, 저장된 영화 정보를 먼저 검색하고 관련 정보가 없을 경우 TMDB에서 영화를 검색하여 새로운 정보를 Vector Store에 저장합니다.

---

## 🛠 기술 스택

| 구분        | 기술                               |
| --------- | -------------------------------- |
| Backend   | Spring Boot                      |
| Language  | Java 21                          |
| AI Model  | Ollama - `llama3.2`              |
| Embedding | Ollama Embedding                 |
| RAG       | Vector Store + Cosine Similarity |
| Movie API | TMDB API                         |
| Discord   | JDA                              |
| Build     | Maven                            |

---

## 🏗 시스템 구조

```text
Discord
   │
   │ !영화 오디세이
   ▼
DiscordBot
   │
   ▼
RagService
   │
   ├── VectorStore 검색
   │       │
   │       ├── 관련 영화 존재
   │       │       ↓
   │       │   MovieDocument
   │       │       ↓
   │       │   Context 생성
   │       │       ↓
   │       │   Ollama
   │       │
   │       └── 관련 영화 없음
   │               ↓
   │             TMDB 검색
   │               ↓
   │          영화 선택
   │               ↓
   │       VectorStore 저장
   │               ↓
   │          Context 생성
   │               ↓
   │             Ollama
   │               ↓
   │           llama3.2
   │               ↓
   │            Discord
```

---

## 🔄 영화 검색 및 RAG 처리 과정

### 1. Discord에서 영화 질문 입력

```text
!영화 오디세이
```

DiscordBot이 메시지를 전달받고 `RagService.generateAnswer()`를 호출합니다.

---

### 2. Vector Store 검색

먼저 사용자의 질문을 Embedding 벡터로 변환합니다.

```java
float[] queryEmbedding = embeddingService.createEmbedding(question);
```

변환된 질문 벡터와 Vector Store에 저장된 영화들의 Embedding 벡터를 **Cosine Similarity**로 비교합니다.

```text
사용자 질문
    ↓
Embedding
    ↓
질문 벡터
    ↓
Vector Store
    ↓
Cosine Similarity
    ↓
관련 영화 검색
```

---

## 🎯 유사도 임계값 적용

초기에는 낮은 유사도 기준을 사용했지만, 실제 Discord 테스트 과정에서 관련성이 낮은 영화가 검색되는 문제가 발생했습니다.

예를 들어 `!영화 인셉션`을 입력했을 때 Vector Store에 `오디세이`만 저장되어 있는 상태에서 다음과 같은 결과가 발생했습니다.

```text
Discord 메시지 : !영화 인셉션

[Vector Store] 저장된 문서 수: 1
[Vector Store] 오디세이 similarity = 0.6596
[Vector Store] 검색 결과 추가: 오디세이
```

`0.60`을 기준으로 설정하면 `인셉션`과 `오디세이`처럼 실제로 다른 영화도 검색 결과에 포함될 수 있었습니다.

이를 개선하기 위해 유사도 기준을 `0.80`으로 높였습니다.

```java
private static final double SIMILARITY_THRESHOLD = 0.80;
```

이후에는 낮은 유사도의 문서를 Vector Store 검색 결과에서 제외하고, 관련 영화가 없으면 TMDB 검색 단계로 넘어가도록 구성했습니다.

```text
Similarity >= 0.80
        │
        ├── YES → Vector Store 정보 사용
        │
        └── NO  → TMDB 검색
```

---

## 🌐 TMDB Fallback 구조

Vector Store에서 질문과 충분히 유사한 영화 정보를 찾지 못한 경우 TMDB API를 이용하여 영화를 검색합니다.

```java
List<MovieDto> movies = movieService.searchMovies(question);
```

예를 들어 처음 `오디세이`를 검색했을 때 Vector Store에 관련 정보가 없으면 TMDB API에서 영화를 검색합니다.

```text
[RAG] VectorStore에 관련 영화가 없습니다.
[RAG] TMDB에서 영화 검색: 오디세이
[RAG] TMDB 검색 결과 수: 15
```

TMDB 검색 결과에서 질문과 가장 적합한 영화를 선택합니다.

```java
MovieDto selectedMovie =
        movieService.selectBestMovie(question, movies);
```

---

## 💾 검색된 영화의 Vector Store 저장

TMDB에서 선택된 영화는 이후 동일하거나 유사한 질문에 활용할 수 있도록 Embedding을 생성하여 Vector Store에 저장합니다.

```java
movieService.saveMovieToVectorStore(selectedMovie);
```

처리 과정은 다음과 같습니다.

```text
TMDB 영화 정보
     ↓
영화 정보 Context 생성
     ↓
Embedding 생성
     ↓
MovieDocument 생성
     ↓
VectorStore 저장
```

예:

```text
[Vector Store] Vector Store에 영화 추가: 오디세이
```

이후 다시 `오디세이`에 대해 질문하면 TMDB를 다시 호출하지 않고 Vector Store를 우선 검색할 수 있습니다.

---

## 🤖 Ollama를 이용한 답변 생성

검색된 영화 정보는 Context 형태로 변환되어 Ollama에 전달됩니다.

```text
영화 제목: 오디세이
개봉일: 2026-08-05
평점: 8.0
줄거리:
10년간 이어진 트로이 전쟁을 승리로 이끈 영웅...
```

이 Context와 사용자의 질문을 `llama3.2` 모델에 전달하여 최종 답변을 생성합니다.

```text
Vector Store / TMDB
        ↓
      Context
        ↓
   OllamaService
        ↓
      llama3.2
        ↓
     AI 답변
        ↓
      Discord
```

---

## 🧩 주요 클래스 역할

### `DiscordBot`

Discord 메시지를 수신하고 영화 명령어를 처리합니다.

```text
!영화 <질문>
```

입력된 질문을 `RagService`로 전달합니다.

---

### `RagService`

전체 RAG 처리 흐름을 담당합니다.

주요 역할:

* Vector Store 검색
* 유사도 임계값 적용
* 검색 결과 Context 생성
* Vector Store에 정보가 없을 경우 TMDB 검색
* 검색된 영화 선택
* 영화 정보를 Vector Store에 저장
* Ollama를 통한 최종 답변 생성

---

### `MovieService`

TMDB 영화 데이터와 Vector Store 사이의 연결 역할을 담당합니다.

주요 기능:

```text
TMDB 영화 검색
        ↓
영화 선택
        ↓
Embedding 생성
        ↓
MovieDocument 생성
        ↓
VectorStore 저장
```

---

### `VectorStoreService`

영화 정보를 메모리 기반 Vector Store에 저장하고 검색합니다.

주요 기능:

* 영화 문서 추가
* 저장된 문서 조회
* 문서 개수 확인
* Vector Store 초기화
* Cosine Similarity 기반 검색
* 유사도 기준에 따른 결과 필터링
* 유사도 내림차순 정렬

---

### `EmbeddingService`

영화 정보와 사용자의 질문을 Embedding 벡터로 변환합니다.

```text
"오디세이"
    ↓
Embedding
    ↓
float[]
```

영화 문서와 질문을 동일한 방식으로 벡터화하여 유사도를 계산할 수 있도록 구성했습니다.

---

## 🐛 개발 과정에서 해결한 문제

### 1. Vector Store 검색 결과의 `null` 문제

초기 RAG 검색 과정에서 검색 결과의 `MovieDocument`가 `null`이 되는 문제가 발생했습니다.

```text
java.lang.NullPointerException:
Cannot invoke "MovieDocument.getTitle()"
because "document" is null
```

문제가 발생한 위치:

```text
RagService.createContextFromResults()
```

검색 결과를 Context로 변환하기 전에 `MovieDocument`가 실제로 존재하는지 확인하도록 방어 로직을 추가했습니다.

```java
MovieDocument document = result.getDocument();

if (document == null) {
    System.out.println("[RAG] 경고: MovieDocument가 null입니다.");
    continue;
}
```

이를 통해 잘못된 검색 결과가 전체 RAG 처리 과정에 영향을 주지 않도록 개선했습니다.

---

### 2. TMDB URL 구성 문제

TMDB API 호출 과정에서 다음 오류가 발생했습니다.

```text
UnknownHostException: Failed to resolve 'search'
```

WebClient가 `search`를 호스트 이름으로 인식하면서 발생한 문제였으며, TMDB 기본 URL과 API 경로를 올바르게 구성하여 해결했습니다.

---

### 3. Vector Store 유사도 기준 조정

Vector Store의 유사도 기준을 실제 Discord 환경에서 테스트했습니다.

`0.60` 기준에서는 저장된 영화가 하나뿐인 상황에서도 관련성이 낮은 질문에 해당 영화가 검색되는 문제가 확인되었습니다.

```text
!영화 인셉션

오디세이 similarity = 0.6596
→ 검색 결과에 오디세이 포함
```

이를 개선하기 위해 현재 유사도 임계값을 `0.80`으로 설정했습니다.

```java
private static final double SIMILARITY_THRESHOLD = 0.80;
```

현재 구조에서는 기준을 충족하는 영화가 없으면 TMDB를 검색하도록 구성하여 **잘못된 Vector Store 검색 결과를 그대로 LLM에 전달하는 문제를 줄이는 방향**으로 개선했습니다.

---

## 📌 현재 RAG 처리 구조

```text
사용자 질문
     │
     ▼
Embedding 생성
     │
     ▼
Vector Store 검색
     │
     ├── similarity >= 0.80
     │          │
     │          ▼
     │     관련 영화 Context
     │          │
     │          ▼
     │       Ollama
     │
     └── similarity < 0.80
                │
                ▼
             TMDB 검색
                │
                ▼
          영화 후보 선택
                │
                ▼
        Vector Store 저장
                │
                ▼
          영화 Context 생성
                │
                ▼
             Ollama
                │
                ▼
             Discord
```

---

## 🚧 향후 개선 예정

* [ ] Vector Store 영속화
* [ ] 애플리케이션 재시작 후에도 영화 데이터 유지
* [ ] 영화 중복 저장 방지
* [ ] 영화 제목 기반 검색과 Semantic Search 결합
* [ ] TMDB 검색 결과의 영화 선택 로직 개선
* [ ] 영화 상세 정보 API 연동
* [ ] 장르 / 감독 / 배우 정보 추가
* [ ] 영화 포스터 Discord 출력
* [ ] RAG Context 품질 개선
* [ ] Ollama 답변 형식 개선
* [ ] 다양한 영화 질문을 통한 검색 품질 테스트
* [ ] Discord 명령어 기능 확장

---

## 📊 현재 테스트

### 영화 검색

```text
!영화 오디세이
```

처리 결과:

```text
Vector Store 검색
      ↓
관련 영화 없음
      ↓
TMDB 검색
      ↓
오디세이 선택
      ↓
Vector Store 저장
      ↓
Ollama 답변 생성
      ↓
Discord 출력
```

### 유사 영화 오검색 테스트

```text
!영화 인셉션
```

Vector Store에 `오디세이`만 존재하는 경우:

```text
오디세이 similarity = 0.6596
```

`0.60` 기준에서는 검색 결과로 포함되었지만, `0.80` 기준에서는 제외되어 TMDB 검색으로 넘어가는 구조입니다.

이를 통해 단순히 유사도 기준을 낮게 설정하기보다 **검색 결과의 관련성을 확인하고 적절한 경우에만 RAG Context로 사용하는 구조**를 구현하고 있습니다.
