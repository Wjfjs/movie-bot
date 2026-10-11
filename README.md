# 🎬 Discord Movie Bot

Discord에서 사용자의 영화 관련 질문을 받아 **TMDB 영화 정보 + RAG + Ollama**를 활용하여 영화 정보를 제공하는 AI 영화 정보 봇입니다.

사용자가 Discord에서 `!영화` 명령어와 함께 영화 제목이나 영화 관련 질문을 입력하면, Vector Store에 저장된 영화 정보를 우선 검색합니다. 관련 정보가 없을 경우 TMDB에서 영화를 검색하고, 검색된 영화 정보를 Embedding하여 저장한 뒤 Ollama를 통해 자연어 답변을 생성합니다.

현재는 메모리 기반 Vector Store를 사용하고 있으며, PostgreSQL과 pgvector를 이용한 영구 저장 및 벡터 검색으로 전환하는 작업을 진행하고 있습니다.

---

## 📌 프로젝트 개요

### 프로젝트 목표

- Discord에서 영화 정보를 자연어로 질문
- TMDB API를 활용한 영화 정보 검색
- 영화 정보를 Embedding하여 Vector Store에 저장
- RAG를 이용하여 질문과 관련된 영화 정보 검색
- Ollama의 `llama3.2` 모델을 이용한 자연어 답변 생성
- Vector Store에 없는 영화는 TMDB를 통해 자동 검색 및 저장
- PostgreSQL과 pgvector를 이용한 영화 데이터 및 Embedding의 영구 저장

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

※ 출력 예시는 형식 설명을 위한 예시입니다.

---

## 🛠️ 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Discord API | JDA |
| Movie API | TMDB API |
| LLM | Ollama - llama3.2 |
| Embedding | Ollama 기반 Embedding 모델 |
| RAG | Embedding + Vector Store |
| Database | PostgreSQL 17 |
| Vector Search | pgvector |
| Database Access | Spring JDBC, JdbcTemplate |
| HTTP Client | Spring WebClient |
| Container | Docker |
| Build | Maven |
| IDE | VS Code |

---

## 🏗️ 시스템 구조

### 현재 구현된 RAG 처리 흐름

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
   ├── 영화 제목 검색
   │       │
   │       ├── 제목 일치 결과 있음
   │       │       ↓
   │       │   영화 Context 생성
   │       │
   │       └── 제목 일치 결과 없음
   │               ↓
   │          Embedding 검색
   │               │
   │               ├── 유사도 기준 충족
   │               │       ↓
   │               │   영화 Context 생성
   │               │
   │               └── 관련 정보 없음
   │                       ↓
   │                    TMDB 검색
   │                       ↓
   │                  영화 선택 및 저장
   │                       ↓
   │                  영화 Context 생성
   │
   ▼
OllamaService
   │
   ▼
llama3.2
   │
   ▼
Discord 답변
```

### 향후 목표 구조

현재 메모리에 저장하는 영화 문서와 Embedding을 PostgreSQL로 이전할 예정입니다.

```text
Discord
   ↓
DiscordBot
   ↓
RagService
   ↓
VectorStoreService
   ├── 영화 제목 검색
   ├── pgvector 기반 유사도 검색
   ├── 영화 정보 저장
   └── 영화 정보 조회
           ↓
   PostgreSQL + pgvector
           ↓
     검색 결과 Context
           ↓
        Ollama
           ↓
       llama3.2
           ↓
     Discord 답변
```

이를 통해 애플리케이션을 재시작하더라도 저장된 영화 정보와 Embedding을 유지할 수 있도록 개선할 계획입니다.

---

## 🔄 RAG 처리 과정

사용자가 영화 관련 질문을 입력하면 다음 과정으로 처리됩니다.

### 1. 영화 제목 검색

먼저 Vector Store에서 영화 제목이 일치하는 문서를 검색합니다.

```text
사용자 질문
    ↓
영화 제목 정규화
    ↓
저장된 영화 제목과 비교
    ↓
일치하는 영화가 있는가?
```

영화 제목은 공백 및 대소문자 차이를 정규화하여 비교합니다. 제목이 정확하게 일치하면 해당 영화 정보를 우선적으로 사용합니다.

### 2. 사용자 질문 Embedding

제목 검색 결과가 없으면 사용자의 질문을 Embedding 모델을 이용하여 벡터로 변환합니다.

```text
"우주를 배경으로 한 SF 영화"
              ↓
          Embedding
              ↓
    [0.12, -0.34, 0.56, ...]
```

### 3. Vector Store 검색

질문 벡터와 저장된 영화 벡터 사이의 Cosine Similarity를 계산합니다.

```text
사용자 질문
    ↓
Embedding
    ↓
Cosine Similarity
    ↓
저장된 영화들과 유사도 비교
    ↓
유사도 임계값 적용
    ↓
관련 영화 검색 결과 반환
```

유사도가 높은 영화부터 정렬하고, 설정된 임계값을 충족하는 영화만 관련 검색 결과로 사용합니다.

현재는 애플리케이션 메모리에서 유사도를 계산합니다. 향후 PostgreSQL의 pgvector를 활용하여 데이터베이스에서 직접 유사도 검색을 수행하도록 변경할 예정입니다.

---

## 🔍 유사도 임계값 개선

초기에는 유사도 임계값을 `0.60`으로 설정했습니다.

하지만 테스트 과정에서 관련 없는 영화도 검색 결과로 판단되는 문제가 발생했습니다.

예를 들어 Vector Store에 `오디세이`가 저장된 상태에서 사용자가 `인셉션`을 검색했을 때에도 유사도가 약 `0.65`로 측정되어 기존 영화가 검색되는 문제가 있었습니다.

```text
인셉션
    ↓
Vector Store 검색
    ↓
오디세이 similarity = 0.659...
    ↓
0.60 이상
    ↓
관련 영화로 잘못 판단
```

이를 개선하기 위해 유사도 임계값을 `0.80`으로 변경했습니다.

```java
private static final double SIMILARITY_THRESHOLD = 0.80;
```

변경 후에는 임계값보다 낮은 유사도의 영화가 검색 결과에서 제외되고, 관련 영화가 없을 경우 TMDB 검색으로 넘어가도록 구성했습니다.

```text
질문
  ↓
영화 제목 검색
  ↓
제목 일치 결과가 있는가?
  ├─ YES → 제목 일치 영화 사용
  │
  └─ NO → Embedding 검색
                ↓
        Similarity >= 0.80 ?
          ├─ YES → 관련 영화 정보 사용
          └─ NO  → TMDB 검색
```

이를 통해 단순히 가장 유사한 영화를 반환하는 것이 아니라, 설정된 관련성 기준을 충족하는 경우에만 기존 RAG 데이터를 사용하도록 개선했습니다.

※ `0.80`은 현재 프로젝트의 테스트를 바탕으로 설정한 값이며, 모든 질문에서 정확성을 보장하는 절대적인 기준은 아닙니다.

---

## 🎬 TMDB Fallback 처리

Vector Store에서 관련 영화 정보를 찾지 못하면 TMDB API를 이용하여 영화를 검색합니다.

```text
Vector Store 검색
       ↓
관련 영화 없음
       ↓
    TMDB API
       ↓
  영화 검색 결과
       ↓
  가장 적합한 영화 선택
       ↓
   Embedding 생성
       ↓
  Vector Store 저장
       ↓
  영화 Context 생성
       ↓
      Ollama
```

처음 검색하는 영화라면 TMDB에서 영화 정보를 가져와 Embedding한 뒤 Vector Store에 저장합니다.

이후 동일한 영화에 대한 질문이 들어오면 저장된 정보를 재사용할 수 있습니다.

다만 현재 Vector Store는 메모리 기반이므로, 애플리케이션을 재시작하면 저장된 문서가 초기화됩니다. PostgreSQL 영구 저장 기능을 구현하면 이 부분을 개선할 수 있습니다.

---

## 🤖 Ollama를 이용한 답변 생성

검색된 영화 정보를 Context로 구성한 후 Ollama의 `llama3.2` 모델에 전달합니다.

```text
검색된 영화 정보
       ↓
  영화 Context 생성
       ↓
    OllamaService
       ↓
      Ollama
       ↓
     llama3.2
       ↓
    자연어 답변
       ↓
      Discord
```

Ollama에는 사용자의 질문과 검색된 영화 정보를 함께 전달하여, 검색된 데이터를 바탕으로 답변하도록 구성했습니다.

---

## 📂 주요 클래스

### `DiscordBot`

Discord 메시지를 수신하고 `!영화` 명령어를 처리합니다. 사용자 질문을 `RagService`에 전달하고 생성된 답변을 Discord 채널에 전송합니다.

### `RagService`

전체 RAG 처리 흐름을 담당합니다.

- 영화 제목 검색
- Embedding 기반 검색
- 유사도 임계값 판단
- TMDB fallback
- 영화 Context 구성
- Ollama 답변 생성 요청

### `MovieService`

TMDB 검색 결과에서 적합한 영화를 선택하고, 영화 정보를 Context 문자열로 구성합니다. 또한 영화 정보를 Embedding하여 Vector Store에 저장하는 작업을 담당합니다.

### `TmdbService`

TMDB API와 통신하여 영화 검색 결과를 가져옵니다.

### `EmbeddingService`

영화 정보와 사용자 질문을 벡터로 변환합니다.

### `VectorStoreService`

현재는 메모리 기반으로 영화 문서와 Embedding을 관리합니다. 제목 검색, Cosine Similarity 계산, 검색 결과 정렬 기능을 제공합니다.

향후 PostgreSQL 및 pgvector를 활용하는 DB 기반 저장소로 전환할 예정입니다.

### `OllamaService`

검색된 영화 Context를 Ollama의 `llama3.2` 모델에 전달하여 최종 답변을 생성합니다.

---

## 🗄️ PostgreSQL 및 pgvector

Vector Store의 영속성을 확보하기 위해 PostgreSQL과 pgvector를 활용한 데이터베이스 환경을 구성하고 있습니다.

### 1. Docker 기반 PostgreSQL 실행

PostgreSQL과 pgvector가 포함된 Docker 이미지를 사용합니다.

```text
Docker
  └── PostgreSQL 17 + pgvector
          └── moviebot 스키마
                  └── movie_documents 테이블
```

Docker를 이용하면 PostgreSQL을 Windows에 직접 설치하는 대신 컨테이너에서 실행할 수 있으며, 다른 컴퓨터에서도 유사한 DB 환경을 재구성하기 쉽습니다.

현재 Docker 컨테이너는 로컬 PC에서 실행 중입니다.

### 2. 데이터베이스 테이블

현재 생성한 `moviebot.movie_documents` 테이블의 구조는 다음과 같습니다.

| 컬럼 | 데이터 타입 | 설명 |
|---|---|---|
| `id` | `BIGSERIAL` | 기본 키 |
| `title` | `VARCHAR(255)` | 영화 제목 |
| `content` | `TEXT` | 영화 정보 및 줄거리 |
| `embedding` | `VECTOR(768)` | 768차원 Embedding 벡터 |
| `created_at` | `TIMESTAMP` | 데이터 생성 시각 |

## 🎯 프로젝트 핵심

이 프로젝트에서는 단순히 API에서 영화 정보를 가져와 출력하는 방식에서 벗어나, RAG 기반의 영화 정보 검색 및 생성 구조를 구현했습니다.

```text
사용자 질문
     ↓
영화 제목 검색
     ↓
Embedding 기반 검색
     ↓
유사도 판단
     ↓
관련 정보가 있으면 RAG
     ↓
없으면 TMDB 검색
     ↓
영화 정보 및 Embedding 저장
     ↓
Ollama
     ↓
자연어 답변
```

특히 검색 결과를 무조건 사용하는 것이 아니라 유사도 임계값을 적용하여 관련성이 낮은 데이터를 제외하고 TMDB 검색으로 전환하는 fallback 구조를 구현했습니다.

또한 Docker 기반 PostgreSQL + pgvector 환경을 구성하고 Spring Boot의 JDBC 연결을 확인하여, 향후 벡터 데이터를 영구 저장하고 DB에서 직접 검색할 수 있는 기반을 마련했습니다.

현재는 메모리 기반 Vector Store를 PostgreSQL 기반으로 전환하는 작업을 진행하고 있습니다.
