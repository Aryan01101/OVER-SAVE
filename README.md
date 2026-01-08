<br />
<div align="center">
    <i alt="OVER-SAVE logo" width="72">
    <h3 align="center">OVER-SAVE</h3>
    </i>
    <p align="center">
        
        A proactive, gamified budgeting companion built with Spring Boot.
    </p>
</div>

Managing money is harder than ever, especially for young adults, students, and people living paycheck to paycheck. Rising costs of living mean that even small emergencies or impulsive spending can cause major financial stress. Existing finance apps are often too complicated, too time-consuming, or designed for people who are already financially savvy, leaving many without accessible tools to build healthier financial habits. <em>OVER-SAVE</em> was created to close this gap by making budgeting approachable, supportive, and motivating.

<em>OVER-SAVE</em> is a budget-tracking application that combines essential features with unique, behaviour-changing tools. Users can track expenses and income, set monthly budgets, manage subscriptions, define savings goals, and view progress through clear dashboard visualisations. <em>OVER-SAVE</em> introduces gamification through BudgetCoins, which reward good financial habits and can be redeemed for perks. Together, these features make budgeting not just about tracking, but about helping people save smarter and stay on track.

### Features
- **Expense & Income Tracking:** record transactions with custom categories.
- **Budget Management:** set and monitor monthly budgets to stay on target.
- **Savings Goals:** define goals and track progress over time.
- **Subscription Tracking:** keep tabs on recurring payments and avoid surprises.
- **Financial Dashboard:** view clear visualisations of spending, saving, and trends.
- **Data Import/Export:** back up or transfer your financial data easily.
- **Gamification with BudgetCoins:** earn and redeem rewards for building positive financial habits.

![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Gradle](https://img.shields.io/badge/gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)
![Jira](https://img.shields.io/badge/Jira-0052CC?style=for-the-badge&logo=Jira&logoColor=white)

# Getting started

This application runs using Java 17 and Gradle 9.1 alongside PostgreSQL 17.

1. Install PostgreSQL 17

    For MacOS:

    ```bash
    brew install postgresql@17
    echo 'export PATH="/opt/homebrew/opt/postgresql@17/bin:$PATH"' >> ~/.zshrc
    source ~/.zshrc
    brew services start postgresql@17

    # Check installation
    psql --version
    ```
    To start a PostgresSQL service:
    ```sudo service postgresql start```

2. Set up user and database

    Ensure that the user `postgres` exists on your system by running `psql -U postgres` in your terminal. If an error occurs, set up the user by running the following commands:
    ```
    psql -d postgres
    postgres=# CREATE USER postgres WITH SUPERUSER PASSWORD 'replace-me';
    postgres=# \q
    ```

    The `postgres` user should now be set up. To set up the database:

    ```
    psql -U postgres or sudo -u postgres psql
    postgres=# CREATE DATABASE unhinged;
    ```

3. Navigate to `project` and run:

    ```bash
    ./gradlew bootRun
    ```

The default base URL is `http://localhost:8080/`.

4. To run all tests and produce a coverage report, run:

    ```bash
    gradle jacocoTestReport
    ```

This should produce a `html` report in `project/build/reports/jacoco/test/html`.

## Application configuration

System-wide defaults live in `project/src/main/resources/application.properties`. Before running locally or deploying:

- Create `project/src/main/resources/application-local.properties` (ignored by Git). 
- Mandatory overrides:
  - `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
  - `spring.mail.username`, `spring.mail.password`
  - OAuth client secrets: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
  - `APP_SESSION_SECRET` for signing session tokens
- Example local override file:
  ```properties
  spring.datasource.url=jdbc:postgresql://localhost:5432/elec5619
  spring.datasource.username=oversave
  spring.datasource.password=strong-password
  spring.mail.username=your.address@example.com
  spring.mail.password=app-specific-password
  app.session.secret=replace-with-random-string
  ```

## API documentation

Visit our [API documentation](API.md) page for more details on our REST API.


## Repository layout
| Path | Description |
| --- | --- |
| `project/src/main/java` | Spring Boot source (controllers, services, config, entities). |
| `project/src/main/resources` | Application properties and static assets. |
| `project/src/test/java` | Unit and integration tests. |
| `docs/` | Branding assets and sample CSVs for imports. |
| `API.md` | Detailed REST endpoint catalogue. |

# Contact

```
| name            |
|-----------------|
| Aryan Adhikari  |
| Antriksh Dhand  |
| Kartik Nair     |
| Taozhao Chen    |
| Joey Chen       |

```
