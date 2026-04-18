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

## Posicao sobre open data

Portugal Towers defende que a localizacao das estacoes base e torres de telecomunicacoes deve existir em open data, num formato publico, descarregavel, reutilizavel e atualizado regularmente.

A ANACOM publica informacao estatistica agregada sobre redes moveis, incluindo numeros de estacoes 5G por operador, concelho ou freguesia. Essa informacao e util, mas nao resolve o problema principal: os cidadaos, investigadores, autarquias e comunidades tecnicas continuam sem uma base aberta e pratica com a localizacao das infraestruturas de rede.

Esta falta de transparencia prejudica:

- verificacao independente da cobertura real;
- investigacao academica e jornalistica;
- planeamento municipal e analise territorial;
- projetos comunitarios como CellMapper, OpenStreetMap e Portugal Towers;
- confianca publica na evolucao das redes moveis.

A existencia de uma base aberta nao obriga a expor informacao sensivel de seguranca ou configuracoes internas das redes. Bastaria publicar dados civicos essenciais, como localizacao aproximada, operador, tecnologia declarada, estado ativo/inativo e data de atualizacao.

Enquanto essa informacao nao existir em open data oficial, Portugal Towers assume uma abordagem comunitaria: organizar, visualizar e tornar mais acessiveis dados colaborativos, deixando claro que a app e independente, informativa e nao oficial.

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
