# Licencje i źródła

ProtectionStones i poprawki do jego kodu są udostępniane na GPL v3 lub nowszej zgodnie z nagłówkami upstream. Wydanie zawiera oznaczenie modyfikacji i odpowiadające źródła użyte do pełnej kompilacji. [Licencja](LICENSE) · [Nota o zmianach](NOTICE.txt)

W JAR-ze znajdują się biblioteki poniżej. Archiwum źródeł wydania zawiera ich źródła w podanych wersjach oraz zachowane noty dostępne w paczkach źródłowych.

| Zależność | Licencja | Źródła |
| --- | --- | --- |
| Night Config core i toml 3.6.3 | LGPL 3.0 | [Tag v3.6.3](https://github.com/TheElectronWill/night-config/tree/v3.6.3) |
| bStats Bukkit i Base 3.0.2 | MIT | [Tag v3.0.2](https://github.com/Bastian/bStats-Metrics/tree/v3.0.2) |
| Apache Commons IO 2.16.1 | Apache 2.0 | [Źródła wydania](https://archive.apache.org/dist/commons/io/source/) |
| Apache Commons Lang 3.14.0 | Apache 2.0 | [Źródła wydania](https://archive.apache.org/dist/commons/lang/source/) |
| JSON.simple 1.1.1 | Apache 2.0 | [Źródła i POM](https://repo.maven.apache.org/maven2/com/googlecode/json-simple/json-simple/1.1.1/) |

Kod aplikacji i konfiguracja Maven do ponownego złożenia JAR-a są dostępne w archiwum źródeł. Aby złożyć JAR ze zmodyfikowaną biblioteką Night Config, zbuduj bibliotekę ze źródeł, zainstaluj jej moduły w lokalnym repozytorium Maven pod wybraną wersją, ustaw tę wersję w zależnościach projektu i uruchom `mvn clean verify`.

Spigot API, WorldGuard, WorldEdit, VaultAPI, PlaceholderAPI i LuckPerms są zależnościami `provided`. Ich deklaracje i źródła rozwiązywania są w POM-ie upstream. JUnit i Hamcrest, odziedziczone z deklaracji JSON.simple, są wyłączone z pakowania do JAR-a.
