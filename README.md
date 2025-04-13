# 간단한 웹 애플리케이션 서버

Java로 구현된 경량 웹 애플리케이션 서버입니다.

## 주요 기능

- HTTP/1.1 프로토콜 지원
- 가상 호스트 지원
- 서블릿 컨테이너 기능
- 정적 파일 서빙
- 에러 페이지 처리
- 디렉토리 목록 표시
- MIME 타입 지원

## 요구사항

- Java 17 이상
- Maven 3.9 이상

## 빌드 방법

```bash
mvn clean package
```

## 실행 방법

```bash
java -jar was.jar
```

## 설정

서버 설정은 `server-config.json` 파일을 통해 관리됩니다:

```json
{
    "port": 80,
    "defaultHost": "localhost",
    "hosts": [
        {
            "name": "localhost",
            "httpRoot": "webapp/www/localhost",
            "welcomeFile": "index.html",
            "errorPages": {
                "notFound": "error/404.html",
                "forbidden": "error/403.html",
                "internalError": "error/500.html"
            }
        },
        {
            "name": "example.com",
            "httpRoot": "webapp/www/example",
            "welcomeFile": "index.html",
            "errorPages": {
                "notFound": "error/404.html",
                "forbidden": "error/403.html",
                "internalError": "error/500.html"
            }
        },
        {
            "name": "test.com",
            "httpRoot": "webapp/www/test",
            "welcomeFile": "index.html",
            "errorPages": {
                "notFound": "error/404.html",
                "forbidden": "error/403.html",
                "internalError": "error/500.html"
            }
        }
    ]
}
```

## 디렉토리 구조

```
simple-was/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/
│   │   │       ├── config/
│   │   │       ├── exception/
│   │   │       ├── http/
│   │   │       ├── server/
│   │   │       └── servlet/
│   │   └── resources/
│   └── test/
│       └── java/
└── webapps/
    ├── ROOT/
    └── example/
```
