# Spring Boot App - Accounts and Events

## endpoints
- accounts 
  - list
  - create
  - update
  - statistics: TTL -1
    + query by account_id
    + return
      - day: date
      - type: string
      - count: long
- events: TTL 30 days
  - list
  - publish

## models
- account
  - i:integer
  - name:string
- event
  - happenedAt: Instant
  - type: String ~ enum

## implemtation
### naive impl
find persistent jdbc backend
```sql
create table accounts (id bigint, name text);
create table events (account_id bigint references accounts(id), happenedAt timestamp, type text);
```
would easily handle those performance requirements, but has limits scaling

### kafka impl
could just keep accounts in rlb and then use some events thing  
**lets do event sourcing all the way and see how it goes**
- kafka
  - account
    - topic
    - store accounts[(Integer) > Account]
    - store statistic[(id:int, day:date, event:string) > count]

### spring settings
- webflux - because async
- kafka, kafka-streams not sure which is needed
- springdoc-openapi-webflux-ui
- Testcontainers in case kafka needs it
