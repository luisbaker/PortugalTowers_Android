# Portugal Towers Android

Portugal Towers e uma aplicacao Android nativa para explorar torres e antenas de telecomunicacoes em Portugal.

O projeto atual substitui a antiga app React Native/Expo por uma base Kotlin moderna, com Jetpack Compose, Material Design 3 e armazenamento local em SQLite. O objetivo e manter a app rapida, simples de evoluir e preparada para sincronizacao futura de dados.

## Funcionalidades

- Mapa interativo centrado em Portugal.
- Tiles OpenStreetMap via osmdroid.
- Clustering de torres para manter o mapa fluido.
- Queries SQLite por zona visivel do mapa, evitando carregar todos os pontos em memoria.
- Lista de torres perto da localizacao do utilizador.
- Popups com detalhes da torre, operadores e frequencias separadas por operadora.
- Suporte para MEO, NOS, Vodafone, Digi e PLMN desconhecidos.
- Icones/logotipos das operadoras reutilizados do projeto antigo.
- UI em Jetpack Compose com Material Design 3.

## Dados

A app inclui um CSV local com a base de torres:

```text
app/src/main/assets/portugal_telecom_towers.csv
```

No primeiro arranque, o CSV e importado para SQLite. Depois disso, o mapa e os fluxos principais leem a base SQLite local.

Isto e intencional:

- O CSV serve como fonte de importacao.
- SQLite serve como fonte de leitura em runtime.
- O mapa faz queries por bounding box e clusters agregados.
- A estrutura esta preparada para uma sincronizacao futura sem mudar a UI.

Os dados sao comunitarios e informativos. As localizacoes podem ser aproximadas e podem existir antenas em falta ou desatualizadas.

## Mapa

O mapa usa OpenStreetMap como base padrao atraves de osmdroid. Os pontos das torres sao desenhados com marcadores nativos e clustering:

- Zoom baixo: clusters agregados por celula.
- Zoom alto: torres visiveis dentro da bounding box atual.
- Limite de marcadores detalhados para reduzir lag.
- Botao para centrar Portugal.
- Botao para centrar na localizacao do utilizador quando a permissao existe.
- Bussola integrada.

## Creditos

Portugal Towers e uma app comunitaria e nao oficial.

Em Portugal, os dados publicos oficiais sobre antenas moveis nao estao disponibilizados de forma aberta e completa para reutilizacao direta. Por isso, a app usa dados colaborativos e fontes comunitarias.

Creditos e referencias:

- Comunidade portuguesa CellMapper.
- OpenStreetMap e respetivos colaboradores.
- Operadoras identificadas: MEO, NOS, Vodafone e Digi.
- Grupo Telegram: https://t.me/cellmapperpt

Os tiles e dados do OpenStreetMap estao sujeitos aos termos e atribuicoes do OpenStreetMap:

```text
https://www.openstreetmap.org/copyright
```

## Stack

- Kotlin
- Android Gradle Plugin
- Jetpack Compose
- Material Design 3
- AndroidX Lifecycle + ViewModel
- Navigation Compose
- SQLiteOpenHelper
- osmdroid
- osmbonuspack
- JUnit

## Estrutura

```text
app/src/main/java/com/bakertelekom/portugaltowers/
  data/       CSV parser, repositorio e SQLite
  domain/     modelos e regras simples de dominio
  location/   acesso a localizacao do dispositivo
  ui/         ecras Compose, tema, componentes e mapa
```

## Build

Requisitos:

- Android Studio recente
- JDK compativel com Android Gradle Plugin
- Android SDK instalado

Comandos principais:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat installDebug
```

## Permissoes

A app usa:

- Internet, para carregar tiles do mapa.
- Localizacao aproximada/exata, para mostrar torres perto do utilizador e centrar o mapa.

A localizacao e usada apenas no dispositivo enquanto a app esta ativa.

## Estado do projeto

Este repositorio e a versao Android nativa do Portugal Towers. O antigo projeto React Native/Expo foi usado como referencia para produto, fluxos, dados, creditos e assets, mas a implementacao atual foi migrada para Kotlin/Compose.

Foco atual:

- performance do mapa
- consistencia Material Design 3
- dados locais em SQLite
- codigo simples e sustentavel
