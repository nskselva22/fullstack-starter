#!/usr/bin/env bash
# Deploy helper. Run each block separately the first time so you can verify.
#
# Prereqs:
#   - AWS CLI v2 configured (aws configure)
#   - Docker logged into your ECR registry
#   - CloudFormation stack already created (see cloudformation.yml)
#
# Env vars you must set:
#   AWS_REGION         e.g. us-east-1
#   AWS_ACCOUNT_ID     your 12-digit AWS account id
#   APP_NAME           e.g. fullstack-starter
#   ECS_CLUSTER        the ECS cluster name (from CF outputs)
#   ECS_SERVICE        the ECS service name
#   CF_DISTRIBUTION_ID CloudFront distribution id (from CF outputs)
#   S3_BUCKET          S3 bucket for the frontend (from CF outputs)

set -euo pipefail

: "${AWS_REGION:?}"; : "${AWS_ACCOUNT_ID:?}"; : "${APP_NAME:?}"
: "${ECS_CLUSTER:?}"; : "${ECS_SERVICE:?}"
: "${CF_DISTRIBUTION_ID:?}"; : "${S3_BUCKET:?}"

REPO="${APP_NAME}-backend"
ECR="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
TAG="$(git rev-parse --short HEAD 2>/dev/null || date +%s)"

echo ">> 1/5 Ensure ECR repo exists"
aws ecr describe-repositories --repository-names "$REPO" --region "$AWS_REGION" >/dev/null 2>&1 \
  || aws ecr create-repository --repository-name "$REPO" --region "$AWS_REGION" >/dev/null

echo ">> 2/5 Login + build + push backend image ($TAG)"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR"
docker build -t "$REPO:$TAG" ./backend
docker tag  "$REPO:$TAG" "$ECR/$REPO:$TAG"
docker tag  "$REPO:$TAG" "$ECR/$REPO:latest"
docker push "$ECR/$REPO:$TAG"
docker push "$ECR/$REPO:latest"

echo ">> 3/5 Force new ECS deployment"
aws ecs update-service \
  --cluster "$ECS_CLUSTER" \
  --service "$ECS_SERVICE" \
  --force-new-deployment \
  --region "$AWS_REGION" >/dev/null

echo ">> 4/5 Build + sync frontend to S3"
(cd frontend && npm install && npm run build)
aws s3 sync ./frontend/dist "s3://$S3_BUCKET/" --delete

echo ">> 5/5 Invalidate CloudFront cache"
aws cloudfront create-invalidation \
  --distribution-id "$CF_DISTRIBUTION_ID" \
  --paths "/*" >/dev/null

echo "Done. Backend: $ECR/$REPO:$TAG"
