# AI Architecture for SalonFlow

## Goal

Create a dedicated AI module that can grow with future features without spreading AI logic across booking, customer, staff, or notification code.

## Current backend context

The project already has strong foundations that AI can reuse:

- Spring Boot 3.5 and Java 21
- PostgreSQL for transactional data
- Redis for short-lived state and caching
- Elasticsearch for search-oriented workloads
- MinIO for file storage
- Scheduler for background jobs
- WebSocket for realtime updates
- Notification and email infrastructure

## Proposed AI module

Recommended package root:

`com.example.salonflow.ai`

Suggested subpackages:

- `ai.config` - AI properties and HTTP client setup
- `ai.controller` - public and admin AI endpoints
- `ai.dto` - request/response objects
- `ai.client` - provider clients such as OpenAI or other LLMs
- `ai.service` - orchestration and use-case interfaces
- `ai.service.impl` - concrete AI use-case logic
- `ai.memory` - conversation state and short-term memory
- `ai.knowledge` - RAG, retrieval, and domain context assembly
- `ai.prompt` - prompt templates and prompt builders
- `ai.safety` - moderation, guardrails, and policy checks
- `ai.event` - async AI events
- `ai.listener` - event consumers
- `ai.enum` - AI use-case enums
- `ai.mapper` - mapping between domain and AI DTOs

## Recommended layers

### 1. AI controller layer

Only receives HTTP requests and delegates to services.

Typical endpoints later:

- chat assistant
- booking suggestion
- customer insight summary
- staff recommendation
- FAQ assistant
- semantic search

### 2. AI service layer

Contains business orchestration:

- chooses the correct AI use case
- loads domain context
- applies safety rules
- calls the provider client
- stores conversation state
- returns normalized response

### 3. Provider client layer

Isolates vendor-specific integration:

- OpenAI
- Azure OpenAI
- local model server
- future provider swap without touching business code

### 4. Knowledge layer

Handles retrieval and grounding:

- branch profile
- service catalog
- booking history
- customer preferences
- staff skills and schedules
- salon policies

This layer can later use Elasticsearch vector search or a dedicated vector store.

### 5. Memory layer

Stores short-term conversation and session context:

- Redis for fast chat memory
- PostgreSQL for durable audit/history if needed

### 6. Safety layer

Adds guardrails:

- prompt injection checks
- PII filtering
- role-based access
- rate limiting
- output validation

## Data flow

1. Client calls `ai.controller`
2. Controller sends request to `ai.service`
3. Service loads context from domain services and repositories
4. Knowledge layer assembles grounded context
5. Prompt layer formats the final prompt
6. Provider client calls the LLM
7. Safety layer validates the output
8. Memory layer stores conversation state
9. Response is returned to client

## Suggested use cases

Start with these AI features first:

- smart booking assistant
- best time suggestion
- staff recommendation
- cancellation risk prediction
- customer segmentation summary
- automated FAQ answer
- service bundle recommendation

## Integration points

The AI module should reuse current systems instead of duplicating them:

- Booking data for time and availability logic
- Staff and service data for recommendations
- Redis for conversation cache and rate limits
- Elasticsearch for semantic retrieval
- MinIO for attachments or uploaded documents
- Notification module for AI-driven alerts
- Scheduler for offline AI jobs

## Rollout strategy

Phase 1:

- create module skeleton
- define DTOs and interfaces
- create config and provider abstraction

Phase 2:

- add booking assistant and recommendation endpoints
- connect Redis memory
- add prompt templates

Phase 3:

- add RAG
- add semantic search
- add monitoring and audit logs

