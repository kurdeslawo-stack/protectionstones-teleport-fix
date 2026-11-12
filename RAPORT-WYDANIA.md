# Raport wydania v2.10.6-teleport-fix.1

## Pełna kompilacja Maven

`mvn -B -ntp clean verify` zakończyło się `BUILD SUCCESS` z Mavenem 3.9.11 i Temurin JDK 25.0.3. Maven skompilował **79 plików Java**, zbudował końcowy JAR z bibliotekami, archiwum źródeł i Javadoc. Surefire wykonał fazę testów. Tag 2.10.6 nie zawiera testów jednostkowych. [GitHub Actions](https://github.com/kurdeslawo-stack/protectionstones-teleport-fix/actions/workflows/maven.yml)

Poprzedni problem z prawami zapisu rozwiązało osobne lokalne repozytorium Maven. Pierwsza pełna próba dotarła do Javadoc i zatrzymała się na braku prawidłowego `JAVA_HOME`. Po wskazaniu pełnego JDK kompilacja i Javadoc zakończyły się poprawnie. W logu zostały ostrzeżenia upstream dotyczące dokumentacji, przestarzałych metod, zduplikowanych zasobów zależności i odpowiedzi 403 ze starego adresu repozytorium Paper. Wymagane artefakty zostały pobrane z innych repozytoriów zadeklarowanych przez upstream. Oddzielnie wykonano pełne `clean verify` ze źródeł wyjętych z archiwum wydania. Układ wpisów i bajty klas w JAR-ze po tej kompilacji są zgodne z wydawanym JAR-em.

Kompilacja używa Spigot API 1.21.10 z upstream (snapshot `20251209.161938-19`). Deskryptor pluginu pozostaje przy `api-version: 1.21.10`, a zmienione klasy mają wersję bajtkodu 60, zgodną z celem Java 16 w POM-ie. Zgodność działania zweryfikowano na **Paper 26.3 build 178 (`03226a5`) i Java 25**. [Wymagania Paper](https://docs.papermc.io/paper/getting-started/)

### Porównanie artefaktów

| JAR | Wpisy plików | Klasy | SHA-256 |
| --- | ---: | ---: | --- |
| Oficjalny 2.10.6, użyty jako punkt odniesienia | — | — | `9a117142f5dfe40140e3620468e6a70022859a8be7a3da422f4e13dc754cfad6` |
| Wcześniejszy częściowo przebudowany JAR | 553 | 513 | `5aa01c2c0dd187299785f9b906ece8aa712fe24e216c6b65d5a5dfb905dd9809` |
| Pełny build wydania | 517 | 501 | `857270e542657e79dc7d69e9230041ab75ef6a71260d63ad8639024ccbb8dc99` |

Wspólne wpisy różnią się w 28 miejscach, w tym 23 pliki klas. Wcześniejszy JAR zachowywał większość klas z gotowego wydania, podczas gdy ten build kompiluje całe drzewo źródeł. Deskryptor pluginu i oba domyślne pliki TOML są równoważne po pominięciu różnic końców linii. Dodano GPL i notę o modyfikacji. Usunięto 38 wpisów pochodzących z testowych zależności JUnit i Hamcrest, w tym 12 plików `package-info.class`. Zmiany Java pozostają w `ArgTp.java` i `PSCommand.java`.

## Próby integracyjne

Użyto tych samych wersji Paper, Java, WorldGuard 7.0.19, WorldEdit 7.4.6-beta-02, FakePlayerPlugin 2.1.0 i tego samego JAR-a fixture. Obie instancje miały świeży syntetyczny świat oraz trzy sekundy oczekiwania z kontrolą ruchu, poza przypadkami zmieniającymi te ustawienia na potrzeby próby.

Na oryginale **19/27**, na pełnym buildzie **27/27**. W każdej komórce kolejno podano wynik, liczbę zdarzeń do działki i sukcesów, ticki od ostatniej komendy do zdarzenia oraz liczbę zadań pozostawionych po próbie. Kreska oznacza brak zdarzenia. Osiem niepowodzeń oryginału wynika z przedwczesnego zakończenia oczekiwania i pozostawionych zadań. Po czterech seriach na oryginale zostało 45 zadań.

| Nr | Scenariusz | Oryginał | Pełny build |
| ---: | --- | --- | --- |
| 1 | Komenda bez argumentu przy jednej działce | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 2 | Bezruch | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 3 | Obrót kamery | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 4 | Przesunięcie o 0,001 bloku na osi X | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 5 | Przesunięcie o 1,0 bloku na osi X | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 6 | Przesunięcie o 1,25 bloku na osi X | OK · 0/0 · — · 0 | OK · 0/0 · — · 0 |
| 7 | Skok podczas oczekiwania | OK · 0/0 · — · 0 | OK · 0/0 · — · 0 |
| 8 | Komenda po ustabilizowaniu pozycji po skoku | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 9 | Ponowienie podczas odliczania | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 10 | Ruch po ponowieniu komendy | OK · 0/0 · — · 0 | OK · 0/0 · — · 0 |
| 11 | Anulowanie zdarzenia przez listener testowy | OK · 1/0 · 61 · 0 | OK · 1/0 · 62 · 0 |
| 12 | Wylogowanie podczas oczekiwania | OK · 0/0 · — · 0 | OK · 0/0 · — · 0 |
| 13 | Nowa próba po ponownym zalogowaniu | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 14 | Próba seryjna 1 | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 15 | Próba seryjna 2 | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 16 | Próba seryjna 3 | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 17 | Próba seryjna 4 | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 18 | Próba seryjna 5 | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 19 | Próba seryjna 6 | OK · 1/1 · 61 · 0 | OK · 1/1 · 62 · 0 |
| 20 | Seria 24 poleceń nr 1 | FAIL · 1/1 · 11 · 9 | OK · 1/1 · 62 · 0 |
| 21 | Seria 24 poleceń nr 2 | FAIL · 1/1 · 6 · 21 | OK · 1/1 · 62 · 0 |
| 22 | Seria 24 poleceń nr 3 | FAIL · 1/1 · 6 · 32 | OK · 1/1 · 62 · 0 |
| 23 | Seria 24 poleceń nr 4 | FAIL · 1/1 · 6 · 45 | OK · 1/1 · 62 · 0 |
| 24 | Pojedyncza komenda po seriach | FAIL · 1/1 · 6 · 45 | OK · 1/1 · 62 · 0 |
| 25 | Uprawnienie omijające odliczanie | FAIL · 1/1 · 2 · 45 | OK · 1/1 · 2 · 0 |
| 26 | Zerowe opóźnienie | FAIL · 1/1 · 2 · 45 | OK · 1/1 · 2 · 0 |
| 27 | Opóźnienie bez kontroli ruchu, przesunięcie o 1,25 bloku | FAIL · 1/1 · 61 · 45 | OK · 1/1 · 62 · 0 |

### Wymuszony konflikt i restart

- **Wymuszony konflikt.** Dwie próby kierowały do różnych działek. Po przesunięciu o 1,25 bloku między komendami gracz stał nieruchomo. Na oryginale starsze zadanie zgłosiło ruch, anulowało aktualną próbę, a scheduler zachował jedno osierocone zadanie. W pełnym buildzie druga działka była jedynym celem, teleportacja nastąpiła raz po 62 tickach i po próbie nie pozostało żadne zadanie.
- **Restart.** W obu wersjach przed zatrzymaniem testu potwierdzono jeden oczekujący teleport. Po ponownym starcie fixture odczytał tę samą działkę i wykonał nową próbę przy zerze starych zadań. Wstępne próby restartu przerwano, gdy FakePlayerPlugin przywracał bota wcześniej niż tester. Testy porównawcze powtórzono z wyłączoną persystencją bota w obu instancjach.

## Audyt kodu

- **Wątek i własność.** Asynchroniczne wyszukanie działki przekazuje rejestrację na główny wątek. Odczyt pozycji, uprawnień i sesji oraz operacje na mapie należą do tego wątku. `PendingTeleport` przechowuje własny licznik, pozycję startową i uchwyt zadania. Sprawdza, czy jest bieżącą próbą, a `remove(uuid, this)` usuwa wpis tylko wtedy, gdy nadal do niego należy.
- **Zakończenie i wyjątki.** Zadanie sprząta swój stan przed wykonaniem teleportacji i przy wyjątku z pracy timera. Weto innego listenera nie zostawia timera. Ponieważ publikacja wpisu następuje po udanym zaplanowaniu, odrzucenie przez scheduler nie tworzy wpisu bez zadania. Ścieżkę odrzucenia zweryfikowano przez przegląd kodu.
- **Cykl życia.** `PSCommand` rejestruje listener używany przez `ArgTp`. Wylogowanie usuwa próbę danej sesji, a spóźniony wynik dla starej sesji odpada przed rejestracją. `PluginDisableEvent` anuluje zadania i czyści mapę. Próby obejmowały wylogowanie, ponowne zalogowanie i czyste zatrzymanie procesu, a przeładowanie pluginu w działającym serwerze sprawdzono przez przegląd kodu.
- **Granice poprawki.** Próby są przyjmowane w kolejności, w której rozwiązane cele trafiają na główny wątek. Sprawdzono timer po rejestracji, a nie gwarancję kolejności zakończenia asynchronicznych wyszukiwań. Audyt nie wykazał kolejnego błędu wymagającego zmiany Java.

Testy dotyczyły izolowanych instancji z syntetycznymi działkami i botem. Nie obejmowały rzeczywistego klienta, zestawu pluginów serwera produkcyjnego ani długotrwałego obciążenia. Wynik potwierdza naprawę odtworzonego wyścigu w laboratorium. Ustalenie przyczyny konkretnych incydentów w innych konfiguracjach wymaga danych z tych incydentów.

[Źródła testów i sposób uruchomienia](integration/README.md) · [Wcześniejszy raport](RAPORT-TESTOW.md) · [Informacje licencyjne](THIRD-PARTY.md)
