# 🎬 MovieInfoBot

Discord에서 사용자의 영화 관련 질문을 받아 **TMDB 영화 정보 + RAG + Ollama**를 활용하여 영화 정보를 제공하는 AI 영화 정보 봇입니다.

사용자가 Discord에서 `!영화` 명령어와 함께 영화 제목이나 영화 관련 질문을 입력하면, Vector Store에 저장된 영화 정보를 우선 검색하고 관련 정보가 없을 경우 TMDB에서 영화를 검색하여 정보를 가져옵니다.

---

## 📌 프로젝트 개요

### 프로젝트 목표

* Discord에서 영화 정보를 자연어로 질문
* TMDB API를 활용한 영화 정보 검색
* 영화 정보를 Embedding하여 Vector Store에 저장
* RAG를 이용하여 질문과 관련된 영화 정보 검색
* Ollama의 `llama3.2` 모델을 이용한 자연어 답변 생성
* Vector Store에 없는 영화는 TMDB를 통해 자동 검색 및 저장

### 사용 예시

```text
!영화 오디세이
```

```text
🎬 오디세이

개봉일: 2026-08-05
평점: 8.0

줄거리:
...
```

---

## 🛠️ 기술 스택

| 구분          | 기술                       |
| ----------- | ------------------------ |
| Language    | Java 21                  |
| Framework   | Spring Boot 4.1.0        |
| AI Model    | Ollama - llama3.2        |
| RAG         | Embedding + Vector Store |
| Movie API   | TMDB API                 |
| Discord     | JDA                      |
| HTTP Client | Spring WebClient         |
| Build       | Maven                    |
| IDE         | VS Code                  |

---

## 🏗️ 시스템 구조

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
   │       ├── 관련 영화 있음
   │       │       ↓
   │       │   영화 Context 생성
   │       │       ↓
   │       │   Ollama
   │       │
   │       └── 관련 영화 없음
   │               ↓
   │             TMDB 검색
   │               ↓
   │          영화 선택 및 저장
   │               ↓
   │          영화 Context 생성
   │               ↓
   │             Ollama
   │
   ▼
llama3.2
   │
   ▼
Discord 답변
```

---

## 🔄 RAG 처리 과정

사용자가 영화 질문을 입력하면 다음과 같은 과정으로 처리됩니다.

### 1. 사용자 질문 Embedding

사용자의 질문을 Embedding 모델을 이용하여 벡터로 변환합니다.

```text
"오디세이"
      ↓
Embedding
      ↓
[0.12, -0.34, 0.56, ...]
```

### 2. Vector Store 검색

변환된 질문 벡터와 Vector Store에 저장된 영화 벡터의 **Cosine Similarity**를 계산합니다.

```text
사용자 질문
    ↓
Embedding
    ↓
Cosine Similarity
    ↓
저장된 영화들과 유사도 비교
```

유사도가 높은 영화부터 정렬한 후 설정된 임계값을 기준으로 관련 영화인지 판단합니다.

---

## 🔍 유사도 임계값 개선

초기에는 유사도 임계값을 `0.60`으로 설정했습니다.

하지만 테스트 과정에서 관련 없는 영화도 검색 결과로 판단되는 문제가 발생했습니다.

예를 들어 Vector Store에 `오디세이`가 저장된 상태에서 사용자가 다른 영화인 `인셉션`을 검색했을 때에도 유사도가 약 `0.65`로 측정되어 기존 영화가 검색되는 문제가 있었습니다.

```text
인셉션
    ↓
Vector Store 검색
    ↓
오디세이 similarity = 0.659...
    ↓
0.60 이상
    ↓
관련 영화로 판단 ❌
```

이를 개선하기 위해 유사도 임계값을 `0.80`으로 변경했습니다.

```java
private static final double SIMILARITY_THRESHOLD = 0.80;
```

변경 후에는 낮은 유사도의 영화가 검색 결과에서 제외되고, 관련 영화가 없을 경우 TMDB 검색으로 넘어가도록 처리했습니다.

```text
질문
 ↓
Vector Store 검색
 ↓
Similarity >= 0.80 ?
 ├─ YES → Vector Store 정보 사용
 │
 └─ NO → TMDB 검색
```

이를 통해 **단순히 가장 유사한 영화를 반환하는 것이 아니라, 일정 수준 이상의 관련성이 있을 때만 기존 RAG 데이터를 사용하도록 개선**했습니다.

---

## 🎬 TMDB Fallback 처리

Vector Store에 관련 영화가 없을 경우 TMDB API를 이용하여 영화를 검색합니다.

```text
Vector Store
     │
     │ 관련 영화 없음
     ▼
   TMDB API
     │
     ▼
영화 검색 결과
     │
     ▼
가장 적합한 영화 선택
     │
     ▼
Vector Store 저장
     │
     ▼
Ollama Context 생성
```

예를 들어 처음 `오디세이`를 질문하면 Vector Store에 해당 영화가 없기 때문에 TMDB에서 영화를 검색합니다.

이후 선택된 영화 정보를 Embedding하여 Vector Store에 저장합니다.

```text
오디세이
   ↓
TMDB 검색
   ↓
영화 정보 획득
   ↓
Embedding
   ↓
Vector Store 저장
```

따라서 동일한 영화에 대한 질문이 반복될 경우 저장된 데이터를 활용할 수 있습니다.

---

## 🤖 Ollama를 이용한 답변 생성

검색된 영화 정보를 Context로 구성한 후 Ollama의 `llama3.2` 모델에 전달합니다.

```text
영화 정보
   ↓
Context 생성
   ↓
Ollama
   ↓
llama3.2
   ↓
자연어 답변
```

Ollama에는 영화 정보와 사용자의 질문을 함께 전달하여 **검색된 데이터를 기반으로 답변하도록 구성**했습니다.

---

## 📂 주요 클래스

### `DiscordBot`

Discord 메시지를 수신하고 `!영화` 명령어를 처리합니다.

```text
Discord
   ↓
DiscordBot
   ↓
RagService
```

### `RagService`

전체 RAG 처리 흐름을 담당합니다.

* Vector Store 검색
* 유사도 임계값 판단
* TMDB fallback
* 영화 Context 생성
* Ollama 호출

### `MovieService`

TMDB에서 영화 정보를 검색하고 Vector Store에 저장할 영화 데이터를 생성합니다.

### `TmdbService`

TMDB API와 통신하여 영화 검색 결과를 가져옵니다.

### `EmbeddingService`

영화 정보와 사용자 질문을 벡터로 변환합니다.

### `VectorStoreService`

영화 문서와 Embedding 벡터를 저장하고 Cosine Similarity를 이용하여 관련 영화를 검색합니다.

### `OllamaService`

검색된 영화 Context를 `llama3.2`에 전달하여 최종 답변을 생성합니다.

---

## 💡 현재 구현된 기능

* [x] Discord 봇 연결
* [x] `!영화` 명령어 처리
* [x] TMDB 영화 검색
* [x] 영화 정보 Embedding
* [x] Vector Store 저장
* [x] Cosine Similarity 기반 검색
* [x] 유사도 임계값 적용
* [x] Vector Store 검색 실패 시 TMDB fallback
* [x] Ollama `llama3.2` 연동
* [x] RAG 기반 영화 답변 생성

---

## 🚀 향후 개선 계획

### 1. Vector Store 영속성 추가

현재 Vector Store는 메모리에 저장되는 구조이기 때문에 서버를 재시작하면 저장된 영화 데이터가 초기화됩니다.

향후 DB 또는 별도의 Vector DB를 적용하여 영화 데이터를 영구적으로 저장할 예정입니다.

### 2. 영화 중복 저장 방지

동일한 영화가 여러 번 검색될 경우 Vector Store에 중복으로 저장되지 않도록 영화 ID 기반 중복 검사를 추가할 예정입니다.

### 3. 영화 제목 검색 + Semantic Search 결합

현재는 Embedding 기반 유사도 검색을 중심으로 구성되어 있습니다.

향후에는 영화 제목의 정확한 일치 여부와 Semantic Search를 함께 활용하여 영화 제목 검색의 정확도를 높일 예정입니다.

```text
영화 제목 정확도
       +
Semantic Similarity
       ↓
최종 영화 검색
```

### 4. 영화 상세 정보 확장

향후 다음과 같은 정보를 추가할 예정입니다.

* 장르
* 감독
* 출연 배우
* 포스터
* 제작 국가
* 연령 등급
* 상세 줄거리

### 5. Discord 명령어 확장

```text
!영화 오디세이
!영화 인셉션
!영화 인터스텔라
```

뿐만 아니라 영화 추천, 장르 검색 등의 기능으로 확장할 예정입니다.

---

## 🎯 프로젝트를 통해 구현한 핵심

이 프로젝트에서는 단순히 API에서 영화 정보를 가져와 출력하는 방식에서 벗어나,

```text
사용자 질문
     ↓
Embedding
     ↓
Vector Store 검색
     ↓
유사도 판단
     ↓
관련 정보가 있으면 RAG
     ↓
없으면 TMDB 검색
     ↓
Vector Store 저장
     ↓
Ollama
     ↓
자연어 답변
```

과 같은 **RAG 기반 영화 정보 검색 및 생성 구조**를 직접 구현했습니다.

특히 Vector Store의 검색 결과를 무조건 사용하는 것이 아니라 **유사도 임계값을 적용하여 관련성이 낮은 데이터를 제외하고 TMDB 검색으로 전환하는 Fallback 구조를 구현**했습니다.
