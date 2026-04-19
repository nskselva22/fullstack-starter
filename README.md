# Fullstack Starter

A production-ready starter for a full-stack app:

- **Frontend** — React 18 + Vite + React Router
- **Backend** — Java 17 + Spring Boot 3 + Gradle
- **Database** — MongoDB (local Docker for dev, Atlas for prod)
- **Auth** — OAuth2 via Spring Security (Google by default; GitHub/Cognito easy to add)
- **Deploy** — AWS ECS Fargate + ALB (backend) and S3 + CloudFront (frontend)

The frontend calls the backend over same-site cookies (Spring session). OAuth is handled server-side — the frontend just redirects to `/oauth2/authorization/google` and reads `/api/auth/user` once the provider redirects back.

## Project layout

```
fullstack-starter/
├── backend/               Spring Boot + Gradle
│   ├── build.gradle
│   ├── src/main/java/com/example/app/
│   │   ├── Application.java
│   │   ├── config/SecurityConfig.java
│   │   ├── controller/{AuthController, TaskController}.java
│   │   ├── model/{User, Task}.java
│   │   ├── repository/{UserRepository, TaskRepository}.java
│   │   └── service/CustomOAuth2UserService.java
│   ├── src/main/resources/application.yml
│   └── Dockerfile
├── frontend/              React + Vite
│   ├── src/
│   │   ├── api/client.js
│   │   ├── context/AuthContext.jsx
│   │   ├── components/{Login, TaskList}.jsx
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── vite.config.js
│   ├── nginx.conf
│   └── Dockerfile
├── infrastructure/        AWS deploy
│   ├── cloudformation.yml ECS + ALB + S3 + CloudFront
│   ├── ecs-task-definition.json
│   └── deploy.sh
├── docker-compose.yml     Local: Mongo + backend + frontend
└── .github/workflows/ci.yml
```

## 1. Prerequisites

- Java 17+
- Node 20+
- Docker (for local Mongo and container builds)
- AWS CLI v2 (for deploy)
- A Google Cloud project for OAuth credentials
- A MongoDB Atlas cluster (free tier works)

## 2. Configure Google OAuth

1. Go to <https://console.cloud.google.com/apis/credentials>.
2. Create an **OAuth 2.0 Client ID** of type **Web application**.
3. Add authorized redirect URIs:
   - `http://localhost:8080/login/oauth2/code/google` — local dev
   - `https://api.yourdomain.com/login/oauth2/code/google` — production
4. Copy the **Client ID** and **Client Secret** into `backend/.env` (see `.env.example`).

## 3. Configure MongoDB Atlas

1. Create a free M0 cluster at <https://cloud.mongodb.com>.
2. Create a database user and add your deploy IPs (or `0.0.0.0/0` for a quick test) to the Atlas Network Access list.
3. Copy the connection string (`mongodb+srv://...`) into `MONGODB_URI`.

For local development, `docker compose up` starts a local Mongo you can use instead.

## 4. Run locally

### Option A: docker compose (one command)

```bash
cp backend/.env.example backend/.env    # add GOOGLE_CLIENT_ID/SECRET
export $(cat backend/.env | xargs)
docker compose up --build
```

- Frontend: <http://localhost:5173>
- Backend:  <http://localhost:8080>

### Option B: native (iterate faster)

```bash
# Terminal 1 — Mongo
docker run --rm -p 27017:27017 mongo:7

# Terminal 2 — backend
cd backend
cp .env.example .env
export $(cat .env | xargs)
./gradlew bootRun   # or: gradle bootRun

# Terminal 3 — frontend
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>, click **Sign in with Google**, and you'll be redirected to Google, back to the backend, and finally to the frontend — authenticated via a session cookie.

## 5. Deploy to AWS

The reference architecture is:

```
Browser → CloudFront → S3 (React static build)
Browser → api.yourdomain.com → ALB → ECS Fargate task (Spring Boot) → MongoDB Atlas
```

### 5a. One-time setup

1. Push the backend image to ECR (see `infrastructure/deploy.sh`).
2. Store secrets in AWS Secrets Manager, one JSON value per secret:
   - `fullstack-starter/MONGODB_URI`
   - `fullstack-starter/GOOGLE_CLIENT_ID`
   - `fullstack-starter/GOOGLE_CLIENT_SECRET`
3. Request an ACM certificate in the ALB's region for `api.yourdomain.com`.
4. Deploy the stack:

   ```bash
   aws cloudformation deploy \
     --stack-name fullstack-starter \
     --template-file infrastructure/cloudformation.yml \
     --capabilities CAPABILITY_IAM \
     --parameter-overrides \
       VpcId=vpc-xxxxxxxx \
       PublicSubnetIds=subnet-aaaa,subnet-bbbb \
       BackendImageUri=$ACCOUNT.dkr.ecr.$REGION.amazonaws.com/fullstack-starter-backend:latest \
       FrontendDomain=app.yourdomain.com \
       CertificateArn=arn:aws:acm:...
   ```

5. In Route 53 (or your DNS):
   - Point `api.yourdomain.com` → the ALB DNS name (CNAME / Alias).
   - Point `app.yourdomain.com` → the CloudFront distribution.
6. Add `https://api.yourdomain.com/login/oauth2/code/google` to the Google OAuth client's redirect URIs.

### 5b. Rolling deploys

```bash
export AWS_REGION=us-east-1
export AWS_ACCOUNT_ID=111111111111
export APP_NAME=fullstack-starter
export ECS_CLUSTER=fullstack-starter-cluster
export ECS_SERVICE=fullstack-starter-service
export CF_DISTRIBUTION_ID=EXXXXXXXXXXXXX
export S3_BUCKET=fullstack-starter-frontend-$AWS_ACCOUNT_ID

./infrastructure/deploy.sh
```

The script builds + pushes the backend image, forces a new ECS deployment, builds the frontend, syncs to S3, and invalidates CloudFront.

## 6. Adding another OAuth provider

Uncomment the GitHub block in `backend/src/main/resources/application.yml`, set `GITHUB_CLIENT_ID`/`GITHUB_CLIENT_SECRET`, and add a button in `frontend/src/components/Login.jsx` that calls `login('github')`. `CustomOAuth2UserService` already normalizes GitHub attributes.

For AWS Cognito, add it as another `registration` entry and set `provider.cognito.issuer-uri`. The same flow works end-to-end.

## 7. API reference

| Method | Path               | Auth | Body                        | Description           |
|--------|--------------------|------|-----------------------------|-----------------------|
| GET    | /api/auth/user     | any  | —                           | Current user profile  |
| POST   | /api/auth/logout   | any  | —                           | End session           |
| GET    | /api/tasks         | yes  | —                           | List my tasks         |
| POST   | /api/tasks         | yes  | `{title, description, done}`| Create task           |
| PUT    | /api/tasks/{id}    | yes  | `{title, description, done}`| Update my task        |
| DELETE | /api/tasks/{id}    | yes  | —                           | Delete my task        |

## 8. Troubleshooting

- **"Invalid redirect URI" from Google** — add the exact URI (`/login/oauth2/code/google`) to the Google OAuth client.
- **CORS error in browser** — the backend's `FRONTEND_URL` env var must exactly match the origin the browser uses (`https://app.yourdomain.com`, no trailing slash).
- **`/api/auth/user` returns 401 after login** — cookies are likely being blocked. In prod, serve frontend and backend from the **same parent domain** (`app.yourdomain.com` + `api.yourdomain.com`) so the session cookie is first-party.
- **Mongo connection timeout** — whitelist the NAT Gateway IP (or ECS task IP) in Atlas Network Access.
