# 추상적 판사님 — Spring Boot(Java 21) → Oracle E2.1.Micro 배포용
# 2단계 빌드: ① Maven으로 jar 빌드 → ② 가벼운 JRE 이미지에 jar만 복사해 실행

# ── 1단계: 빌드 ──
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# 의존성 캐시 최적화: pom.xml만 먼저 복사해 라이브러리 받아두기
COPY pom.xml .
RUN mvn -q dependency:go-offline
# 소스 복사 후 jar 빌드 (테스트는 건너뛰어 빌드 시간 단축)
COPY src ./src
RUN mvn -q clean package -DskipTests

# ── 2단계: 실행 ──
FROM eclipse-temurin:21-jre
WORKDIR /app
# 1단계에서 만든 jar을 가져옴 (이름이 바뀌어도 되게 *.jar로)
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# 1GB RAM 보호: JVM이 메모리를 과하게 잡지 않도록 상한 지정
ENV JAVA_OPTS="-Xmx400m -Xss512k"

# GEMINI_API_KEY는 .env로 주입됨 → application.properties가 환경변수로 덮어쓰게 함
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]