# Poprawka timerów teleportacji w ProtectionStones 2.10.6

Dwie nakładające się próby `/ps home` mogły sprawić, że starszy timer porównał pozycję z pierwszej próby i anulował nowszą. Oba zadania używały tego samego wpisu gracza w mapach licznika i uchwytu. Rejestracja z wątków asynchronicznych mogła zostawić aktywne zadanie po nadpisaniu uchwytu.

Patch przenosi rejestrację i anulowanie na główny wątek Paper. Każda próba ma własny licznik i usuwa tylko swój wpis oraz zadanie. Wyjście gracza i wyłączenie pluginu czyszczą oczekujące próby. Odliczanie, próg ruchu i `protectionstones.tp.bypasswait` zachowują swoje działanie.

To nieoficjalna poprawka dla [ProtectionStones 2.10.6](https://github.com/espidev/ProtectionStones/releases/tag/2.10.6), commit `7701764b5cda7db4ef59d1c99252ac7cc19c98f8`. Weryfikację wykonano na Paper 26.3 build 178 z Java 25.

## Samodzielna kompilacja

Potrzebujesz Git, Bash, Maven 3.9.x i pełnego JDK 25. Ustaw `JAVA_HOME` na katalog JDK, żeby Maven znalazł także `javadoc`. Paper 26.3 uruchom na Java 25. Plugin kompiluje się przeciwko Spigot API 1.21.10 zgodnie z POM-em upstream. [Wymagania Paper](https://docs.papermc.io/paper/getting-started/)

```bash
bash build.sh protectionstones-build
```

Skrypt pobiera tag, sprawdza commit i zastosowanie patcha, a następnie wykonuje `mvn clean verify`. Wynikowy plik to `protectionstones-2.10.6.jar` w katalogu `target` zbudowanego checkoutu. Do użycia osobnego cache można dodać argument `"-Dmaven.repo.local=$(pwd)/.maven-local"`. Przy ponowieniu wybierz nowy katalog checkoutu.

Z pobranego archiwum źródeł wydania możesz też zbudować JAR poleceniem `mvn -B -ntp clean verify` w katalogu zawierającym `pom.xml`. Źródła zawierają już nałożony patch i noty licencyjne.

## Testy i wycofanie

Na wydzielonym Paperze zachowaj kopię oryginalnego JAR-a. Sprawdź poprawkę na zwykłym koncie, z włączoną kontrolą ruchu. Do cofnięcia zatrzymaj instancję testową, przywróć oryginalny plik i uruchom serwer. [Instalacja i wycofanie](INSTALACJA.md)

- [Raport wydania z pełnego buildu](RAPORT-WYDANIA.md)
- [Powtórzenie testów integracyjnych](integration/README.md)
- [Poprzedni raport z testów częściowo przebudowanego JAR-a](RAPORT-TESTOW.md)
- [GPL i zależności](THIRD-PARTY.md)
