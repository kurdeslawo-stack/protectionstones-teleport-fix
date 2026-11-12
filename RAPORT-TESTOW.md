# Raport testów poprawki ProtectionStones 2.10.6

**27/27 scenariuszy integracyjnych przeszło na poprawionym JAR-ze.** Osobno przeszły test wymuszonego wyścigu i test restartu w trakcie odliczania.

## Środowisko i pomiar

Paper 26.3 build 178 (`03226a5`), Java 25.0.3, WorldGuard 7.0.19, WorldEdit 7.4.6-beta-02 i FakePlayerPlugin 2.1.0. Próby wykonywano na izolowanych instancjach, na syntetycznym graczu i syntetycznych działkach. Domyślne ustawienia prób to trzy sekundy oczekiwania z kontrolą ruchu.

Zliczano próby teleportacji do działki, udane teleportacje i zadania pozostałe w schedulerze Paper po scenariuszu. Czas liczono od ostatniej komendy do zdarzenia teleportacji. Kreska oznacza brak zdarzenia do działki.

## 27 scenariuszy integracyjnych

| Nr | Scenariusz | Próby | Udane | Pozostałe zadania | Ticki do zdarzenia | Wynik |
| ---: | --- | ---: | ---: | ---: | ---: | --- |
| 1 | Komenda bez argumentu przy jednej działce | 1 | 1 | 0 | 62 | OK |
| 2 | Bezruch | 1 | 1 | 0 | 62 | OK |
| 3 | Obrót kamery | 1 | 1 | 0 | 62 | OK |
| 4 | Przesunięcie o 0,001 bloku na osi X | 1 | 1 | 0 | 61 | OK |
| 5 | Przesunięcie o 1,0 bloku na osi X | 1 | 1 | 0 | 62 | OK |
| 6 | Przesunięcie o 1,25 bloku na osi X | 0 | 0 | 0 | — | OK |
| 7 | Skok podczas oczekiwania | 0 | 0 | 0 | — | OK |
| 8 | Komenda po ustabilizowaniu pozycji po skoku | 1 | 1 | 0 | 62 | OK |
| 9 | Ponowienie podczas odliczania | 1 | 1 | 0 | 62 | OK |
| 10 | Ruch po ponowieniu komendy | 0 | 0 | 0 | — | OK |
| 11 | Anulowanie zdarzenia przez listener testowy | 1 | 0 | 0 | 62 | OK |
| 12 | Wylogowanie podczas oczekiwania | 0 | 0 | 0 | — | OK |
| 13 | Nowa próba po ponownym zalogowaniu | 1 | 1 | 0 | 62 | OK |
| 14 | Próba seryjna 1 | 1 | 1 | 0 | 62 | OK |
| 15 | Próba seryjna 2 | 1 | 1 | 0 | 62 | OK |
| 16 | Próba seryjna 3 | 1 | 1 | 0 | 62 | OK |
| 17 | Próba seryjna 4 | 1 | 1 | 0 | 62 | OK |
| 18 | Próba seryjna 5 | 1 | 1 | 0 | 62 | OK |
| 19 | Próba seryjna 6 | 1 | 1 | 0 | 62 | OK |
| 20 | Seria 24 poleceń nr 1 | 1 | 1 | 0 | 62 | OK |
| 21 | Seria 24 poleceń nr 2 | 1 | 1 | 0 | 62 | OK |
| 22 | Seria 24 poleceń nr 3 | 1 | 1 | 0 | 62 | OK |
| 23 | Seria 24 poleceń nr 4 | 1 | 1 | 0 | 62 | OK |
| 24 | Pojedyncza komenda po seriach | 1 | 1 | 0 | 62 | OK |
| 25 | Uprawnienie omijające odliczanie | 1 | 1 | 0 | 2 | OK |
| 26 | Zerowe opóźnienie | 1 | 1 | 0 | 2 | OK |
| 27 | Opóźnienie bez kontroli ruchu, przesunięcie o 1,25 bloku | 1 | 1 | 0 | 62 | OK |

## Porównanie z oryginałem

W kontrolowanej regresji dwie komendy miały różne cele. Gracz przesunął się o 1,25 bloku między próbami i stał nieruchomo od drugiej komendy. Debugger wymusił dokończenie dwóch operacji w kolejności A, B przed kolejnym tickiem serwera.

| Próba | Oryginalny JAR | Poprawiony JAR |
| --- | --- | --- |
| Wymuszony wyścig | Pierwszy timer zgłosił ruch, druga próba została anulowana. Zero teleportów do obu celów i jedno osierocone zadanie. | Jeden teleport do drugiego celu po 62 tickach, zero teleportów do pierwszego celu, zero zadań po teście. |
| Zestaw bez debuggera | 18/26. Po seriach poleceń odnotowano teleporty po 31 i 6 tickach oraz do 25 pozostałych zadań. | 27/27. Każda seria zakończyła się jednym teleportem po 62 tickach i zerem pozostałych zadań. Dodatkowy scenariusz obejmował komendę bez argumentu przy jednej działce. |
| Restart w trakcie odliczania | Przed stopem jedno oczekujące zadanie. Po starcie nowa próba przeszła po 61 tickach, zero starych zadań. | Przed stopem jedno oczekujące zadanie. Po starcie nowa próba przeszła po 62 tickach, zero starych zadań. |

Testy potwierdzają zachowanie w opisanym środowisku. Nie ustalają przyczyny konkretnych incydentów w innych konfiguracjach.

## Status kompilacji Maven

**Pełna kompilacja Maven nie została ukończona.** Uruchomienie `mvn test` zakończyło się błędem dostępu do lokalnej pamięci podręcznej podczas rozwiązywania rozszerzenia Sonatype. Tag 2.10.6 nie zawiera zestawu testów jednostkowych.

Na potrzeby testów osobno skompilowano zmienione klasy. W oficjalnym JAR-ze podmieniono `ArgTp.class` i `PSCommand.class` oraz dodano `ArgTp$PendingTeleport.class`. Porównanie archiwów potwierdziło identyczną zawartość wszystkich pozostałych wpisów.

| JAR | SHA-256 |
| --- | --- |
| Oficjalny ProtectionStones 2.10.6 | `9a117142f5dfe40140e3620468e6a70022859a8be7a3da422f4e13dc754cfad6` |
| Poprawiony JAR użyty w testach | `5aa01c2c0dd187299785f9b906ece8aa712fe24e216c6b65d5a5dfb905dd9809` |
