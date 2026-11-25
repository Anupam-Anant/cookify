# Database Structure & Table Definitions

The schema is owned and versioned by **Flyway** (`src/main/resources/db/migration/`) and applied
automatically on startup. Hibernate runs with `ddl-auto: validate` (never `create`/`update`), so the
migrations below are the single source of truth. The same migrations run against **H2** (dev/test,
PostgreSQL-compatibility mode) and **PostgreSQL 16** (prod).

Migrations: `V1` (initial schema, 10 tables) → `V2` (recipe attributes + the OOP discriminator) →
`V4` (saved recipes) → `V5` (indexes + a unique constraint). *(There is no `V3` — a numbering gap, not a
missing migration.)*

## Entity-Relationship Diagram

```mermaid
erDiagram
    app_user ||--o| user_preference : "has (1:1)"
    app_user ||--o{ recipe : "creates"
    recipe ||--|| recipe_category : "categorized by (1:1)"
    recipe ||--o{ recipe_rating : "receives"
    app_user ||--o{ recipe_rating : "gives"
    app_user ||--o{ email_otp : "issued"
    app_user ||--o{ subscription : "subscriber"
    app_user ||--o{ subscription : "creator"
    app_user ||--o{ warning : "receives"
    app_user ||--o{ chat_message : "sends"
    app_user ||--o{ chat_message : "receives"
    app_user ||--o{ saved_recipe : "bookmarks"
    recipe ||--o{ saved_recipe : "bookmarked in"

    app_user {
        bigint user_id PK
        varchar email UK
        varchar username UK
        varchar pass "BCrypt hash"
        varchar phone UK
        boolean two_factor_enabled
        varchar role
        varchar account_status
        int warning_count
    }
    recipe {
        bigint recipe_id PK
        varchar recipe_name
        text ingredient
        text recipe_method
        varchar recipe_video
        varchar image_path
        bigint recipe_creator_id FK
        bigint views
        varchar recipe_type "VEG/NONVEG discriminator"
        int cost
        int speed
        int difficulty
    }
    recipe_category {
        bigint recipe_id PK_FK
        boolean veg_nonveg
        int cooking_time
        int calory
        int protin
    }
    recipe_rating {
        bigint rating_id PK
        bigint recipe_id FK
        bigint user_id FK
        int rate_1
        int rate_5
        text comments
    }
    subscription {
        bigint id PK
        bigint subscriber_id FK
        bigint creator_id FK
    }
    saved_recipe {
        bigint id PK
        bigint user_id FK
        bigint recipe_id FK
    }
    email_otp {
        bigint id PK
        bigint user_id FK
        varchar purpose
        varchar code_hash
        timestamp expires_at
        boolean consumed
    }
    chat_message {
        bigint id PK
        bigint sender_id FK
        bigint recipient_id FK
        text body
    }
    warning {
        bigint id PK
        bigint user_id FK
        varchar reason
    }
    user_preference {
        bigint user_id PK_FK
        boolean veg_noveg
    }
    persistent_logins {
        varchar series PK
        varchar username
        varchar token
    }
```

## Table definitions

### `app_user` — user accounts
| Column | Type | Constraints | Notes |
|---|---|---|---|
| `user_id` | BIGINT | PK, identity | |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | |
| `username` | VARCHAR(100) | NOT NULL, UNIQUE | |
| `name_first` / `name_last` | VARCHAR(100) | | |
| `age` | INT | | |
| `preference_id` | BIGINT | | vestigial (see notes) |
| `gender` | VARCHAR(20) | | |
| `pass` | VARCHAR(100) | NOT NULL | **BCrypt hash**, never plaintext |
| `phone` | VARCHAR(30) | UNIQUE | login identifier |
| `bio` | VARCHAR(500) | | |
| `profile_image_path` | VARCHAR(255) | | served via `/uploads/**` |
| `two_factor_enabled` | BOOLEAN | NOT NULL, DEFAULT FALSE | optional email-OTP MFA |
| `role` | VARCHAR(20) | NOT NULL, DEFAULT 'USER' | USER / MODERATOR / ADMIN |
| `account_status` | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE / SUSPENDED / DELETED |
| `warning_count` | INT | NOT NULL, DEFAULT 0 | drives the 3-strike ban |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now | |

> Renamed from the PDF's `User` because `user` is a reserved word in PostgreSQL.

### `recipe` — recipes (abstract `Recipe` ← `VegRecipe` / `NonVegRecipe`)
| Column | Type | Constraints | Notes |
|---|---|---|---|
| `recipe_id` | BIGINT | PK, identity | |
| `recipe_name` | VARCHAR(255) | NOT NULL | |
| `ingredient` | TEXT | | |
| `recipe_method` | TEXT | | ordered steps |
| `recipe_video` | VARCHAR(255) | | video path |
| `image_path` | VARCHAR(255) | | photo path |
| `recipe_creator_id` | BIGINT | NOT NULL, FK → `app_user(user_id)` | owner |
| `views` | BIGINT | NOT NULL, DEFAULT 0 | popularity signal |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now | |
| `recipe_type` | VARCHAR(10) | NOT NULL, DEFAULT 'VEG' | JPA single-table **discriminator** (VEG/NONVEG) |
| `cost` | INT | | ₹ |
| `speed` | INT | | 1–5 |
| `difficulty` | INT | | 1–5 |

### `recipe_category` — nutrition / cooking metadata (1:1 with `recipe`)
| Column | Type | Constraints | Notes |
|---|---|---|---|
| `recipe_id` | BIGINT | PK, FK → `recipe(recipe_id)` | shared PK |
| `veg_nonveg` | BOOLEAN | | **derived** from `recipe.recipe_type` in the service layer (kept consistent) |
| `cooking_time` | INT | | minutes (filterable) |
| `calory` | INT | | filterable |
| `protin` | INT | | |
| `preference_id` | BIGINT | | vestigial |

### `recipe_rating` — ratings + one comment per (recipe, user)
| Column | Type | Constraints | Notes |
|---|---|---|---|
| `rating_id` | BIGINT | PK, identity | |
| `recipe_id` | BIGINT | NOT NULL, FK → `recipe` | |
| `user_id` | BIGINT | FK → `app_user` | |
| `rate_1` … `rate_5` | INT | DEFAULT 0 | **one-hot** star value (see notes) |
| `comments` | TEXT | | the user's comment on that recipe |
| — | — | **UNIQUE(`recipe_id`,`user_id`)** (`uq_recipe_rating_recipe_user`, V5) | one row per user per recipe |

### `subscription` — follow graph
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, identity |
| `subscriber_id` | BIGINT | NOT NULL, FK → `app_user` |
| `creator_id` | BIGINT | NOT NULL, FK → `app_user` |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now |
| — | — | **UNIQUE(`subscriber_id`,`creator_id`)** |

### `saved_recipe` — bookmarks (V4)
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, identity |
| `user_id` | BIGINT | NOT NULL, FK → `app_user` |
| `recipe_id` | BIGINT | NOT NULL, FK → `recipe` |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now |
| — | — | **UNIQUE(`user_id`,`recipe_id`)** |

### `email_otp` — 2FA & password-reset codes
| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | BIGINT | PK, identity | |
| `user_id` | BIGINT | NOT NULL, FK → `app_user` | |
| `purpose` | VARCHAR(30) | NOT NULL | LOGIN_2FA / PASSWORD_RESET |
| `code_hash` | VARCHAR(100) | NOT NULL | **BCrypt-hashed** OTP |
| `expires_at` | TIMESTAMP | NOT NULL | 10-minute TTL |
| `consumed` | BOOLEAN | NOT NULL, DEFAULT FALSE | single-use; superseded on reissue |
| `attempts` | INT | NOT NULL, DEFAULT 0 | capped at 5 |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now | |

### `warning` — moderation strikes
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, identity |
| `user_id` | BIGINT | NOT NULL, FK → `app_user` |
| `reason` | VARCHAR(255) | |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now |

### `chat_message` — 1:1 direct messages
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, identity |
| `sender_id` | BIGINT | NOT NULL, FK → `app_user` |
| `recipient_id` | BIGINT | NOT NULL, FK → `app_user` |
| `body` | TEXT | NOT NULL |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now |

### `user_preference` — (defined, 1:1 with `app_user`)
`user_id` PK/FK, `veg_noveg` BOOLEAN, `calory` INT, `protin` INT, `pref_recipe_id` VARCHAR, `uploaded_recipe` VARCHAR. *Defined from the PDF design; not yet wired to application logic (see notes).*

### `persistent_logins` — Spring Security remember-me
Standard JDBC token schema: `series` VARCHAR(64) PK, `username` VARCHAR(64), `token` VARCHAR(64), `last_used` TIMESTAMP. Keyed by `username` string (Spring Security contract), not an FK.

## Indexes (V5)

Btree indexes back every frequently-queried FK / filter column:
`recipe(recipe_creator_id)`, `recipe_rating(recipe_id)`, `recipe_rating(user_id)`,
`chat_message(sender_id)`, `chat_message(recipient_id)`, `subscription(creator_id)`,
`subscription(subscriber_id)`, `warning(user_id)`, `saved_recipe(user_id)`,
`saved_recipe(recipe_id)`, `recipe_category(veg_nonveg)` — plus the unique indexes implied by the
`UNIQUE` constraints above.

## Design notes & known trade-offs (transparent)

- **`recipe_type` discriminator** drives JPA `SINGLE_TABLE` inheritance (`Recipe` → `VegRecipe`/`NonVegRecipe`).
  `recipe_category.veg_nonveg` is always derived from it in `RecipeService`, so the two cannot diverge.
- **`recipe_rating` one-hot `rate_1..rate_5`** follows the original assignment's design. It is denormalized;
  a cleaner model is a single `stars SMALLINT CHECK (stars BETWEEN 1 AND 5)`. The `UNIQUE(recipe_id,user_id)`
  constraint (V5) makes the "one rating per user per recipe" invariant DB-enforced rather than app-only.
- **`user_preference` and the `preference_id` columns** are defined from the PDF's data model but are not yet
  consumed by application code — carried for schema fidelity, flagged as a future wiring point.
- **No `ON DELETE CASCADE`** — the 3-strike ban purge (`ModerationService.banAndPurge`) deletes child rows in
  explicit FK-safe order (ratings → category → saved → recipes → subscriptions → chat); a deliberate choice to
  avoid cascade surprises, regression-tested in `BanPurgeSavedRecipeFkTest`.
