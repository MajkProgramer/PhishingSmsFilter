# Phishing SMS Filter

Aplikacja wykrywająca podejrzane wiadomości SMS zawierające phishingowe linki. Użytkownik może wyrazić zgodę na filtrowanie phishingu. Po wyrażeniu takiej zgody, aplikacja odfiltrowuje wiadomości SMS z linkami phishingowymi.
Aplikacja została napisana w Scali oraz wykorzystuje bazę danych PostgreSQL.

---

## Opis i założenia działania aplikacji

Aplikacja przyjmuje wiadomości SMS w postaci JSONów. Przykład wiadomości:
```json
{
  "sender": "48592600600",
  "recipient": "48700800999",
  "message": "Dzień dobry. W związku z audytem nadzór finansowy w naszym banku proszą o potwierdzanie danych pod adresem: https://www.m-bonk.pl.ng/personal-data"
}
```
Następnie sprawdzane jest czy dla numeru podanego w polu 'recipient' aktywna jest subskrypcja filtrowania phishingu.
Jeżeli tak, wtedy wołany jest zewnętrzny serwis sprawdzający linki URL z wiadomości pod kątem phishingu, a rezultat jest zwracany jako odpowiedź aplikacji.
Jeżeli nie, wtedy filtr nie działa, a aplikacja zwraca stosowny komunikat.
Na koniec SMS jest zapisywany do bazy danych i odpowiednio oznaczony pod kątem filtrowania.
W przypadku środowisk nieprodukcyjnych, należy w konfiguracji aplikacji ustawić zmienną is-prod-env na false. Wtedy wołanie zewnętrznego systemu phishingowego jest pomijane, a o odfiltrowaniu wiadomości decyduje, czy w linku znajduje się słowo "phish" czy nie. Ułatwia to testowanie aplikacji.

Dodatkowo aplikacja umożliwia aktywowania subskrypcji lub dezaktywację. Jeżeli na specjalny numer telefonu zostanie przesłany SMS z wiadomością "START"/"STOP", wtedy subskrypcja zostanie aktywowana/dezaktywowana.
Numer, który obsługuje subskrypcję, jest przechowywany w konfiguracji aplikacji i domyślnie ustawiony jako "000111222".

---

## Założenia architektoniczne

* **HTTP API** zbudowane na `http4s`, obsługujące zgłoszenia wiadomości SMS.
* **Modularna architektura**: oddzielone warstwy:

    * API (`SmsRoutes`)
    * Serwisy (`PhishingDetector`, `SubscriptionService`, `SmsStorage`)
    * Warstwa dostępu do danych (`Database`)
* **Konfiguracja przez `pureconfig`** – dane w pliku `application.conf`.
* **PostgreSQL** jako trwała warstwa przechowywania:

    * Tabela `sms` do rejestracji wiadomości i wyników klasyfikacji
    * Tabela `subscriptions` do zarządzania subskrypcjami
* **Dokeryzacja** – łatwe uruchamianie i deployment (kontenery dla aplikacji i bazy danych)
* **Symulacja detekcji phishingu** w środowisku produkcyjnym wykorzystuje API Google WebRisk, w środowisku testowym jest to pomijane

---

## Stack technologiczny

* Scala 2.13
* sbt + sbt-assembly
* Docker
* PostgreSQL
* http4s (REST API)
* pureconfig (konfiguracja)
* circe (serializacja JSON)

---

## Jak uruchomić

### 1. Stwórz sieć Docker:

```bash
docker network create phishing-app-network
```

### 2. Uruchom bazę danych:

```bash
docker run --name postgres-db --network phishing-app-network \
  -e POSTGRES_DB=phishing \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5431:5432 \
  -d postgres:15
```

### 3. Uruchom obraz dockerowy dostępny w Docker Hub:

```bash
docker run --network phishing-app-network -p 8080:8080 majkprogramer/phishing-sms-app
```

---

## Konfiguracja (`application.conf`)

```hocon
app {
  # numer do obsługi subskrypcji
  subscription-phone = "000111222"
  
  # token dostępowy do zewnętrznego systemu wykrywającego phishing
  token = "dummy-token"
  
  # zmienna sterująca odpytywaniem do zewnętrznego systemu
  is-prod-env = false
  
  # konfiguracja bazy danych
  db {
    url = "jdbc:postgresql://postgres-db:5432/phishing"
    user = "postgres"
    password = "postgres"
  }
}
```

---

## Struktura katalogów

```
src/
 └── main/
     ├── scala/
     │    ├── api/              <-- definicje endpointów HTTP
     │    ├── config/           <-- wczytywanie konfiguracji aplikacji
     │    ├── db/               <-- operacje JDBC
     │    ├── main/             <-- punkt wejścia aplikacji
     │    ├── model/            <-- klasy domenowe i JSON
     │    └── service/          <-- logika biznesowa
     └── resources/
          └── application.conf
```

---

## Przykład użycia API dla każdego SMSa

### POST /sms

```json
{
  "sender": "48592600600",
  "recipient": "48700800999",
  "message": "Dzień dobry. W związku z audytem nadzór finansowy w naszym banku proszą o potwierdzanie danych pod adresem: https://www.m-bonk.pl.ng/personal-data"
}
```

**Odpowiedź:**

* "Phishing message detected." – jeśli wykryto zagrożenie
* "Message accepted." – jeśli wiadomość jest bezpieczna
* "User not subscribed." – jeśli odbiorca nie jest subskrybentem

---

## Przykład użycia API przy subskrypcji

### POST /sms

```json
{
  "sender": "48592600600",
  "recipient": "000111222",
  "message": "START"
}
```

**Odpowiedź:**

* "Subscription activated." – jeśli subskrypcja została aktywowana
* "Subscription deactivated." – jeśli subskrypcja została dezaktywowana
* "Unrecognized command." – jeśli wiadomość miała inną treść niż "START"/"STOP"

---

## Licencja

MIT
