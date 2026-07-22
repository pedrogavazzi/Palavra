# Palavra do Dia

App Android nativo (Kotlin + Room) que apresenta uma palavra do dicionário por dia — útil, mas pouco usada no cotidiano — com definição, exemplo, tradução em francês e inglês, notificação em horário configurável, teste semanal (ciclo de 7 dias) e revisão mensal (4 ciclos).

## O que já vem pronto

- **575 palavras** curadas em `app/src/main/assets/words.json` (511 mais eruditas + 575-511 um pouco mais acessíveis, mantendo o objetivo de enriquecer o vocabulário), cada uma com classe gramatical, definição, exemplo de uso e tradução em francês e inglês.
- **Ciclo de 7 dias**: começa no dia em que você abre a primeira palavra e avança por dias corridos (mesmo que você pule um dia).
- **Teste semanal** de múltipla escolha ao final de cada ciclo, usando apenas as palavras que você marcou como aprendidas.
- **Revisão mensal** agrupando 4 ciclos (até 28 palavras), com aproveitamento por mês.
- **Notificação diária exata** (`AlarmManager`), que sobrevive a reinício do aparelho (`BootReceiver`) e se reagenda automaticamente.
- **Navegação por 4 abas** (barra inferior):
  - **Hoje** — a(s) palavra(s) do dia (1 a 3, configurável) e o botão de marcar cada uma como aprendida.
  - **Agenda** — uma linha por dia desde o início do primeiro ciclo até o fim do ciclo atual (incluindo dias futuros ainda sem palavra sorteada), com data e horário de acordo com o agendamento configurado; palavras não aprendidas aparecem na lista (para reforçar o hábito), mas sem revelar o texto.
  - **Treino** — prática livre com as palavras já aprendidas: o usuário escolhe entre relacionar a palavra com o significado, com o francês ou com o inglês. Não altera as estatísticas oficiais.
  - **Desempenho** — progresso geral e revisão mensal; mostra um aviso amigável até você concluir seu primeiro ciclo de 7 dias.
- Configurações com **sub-abas**: "Horário" (fica isolado, só o relógio) e "Preferências" (palavras por dia, tamanho de fonte, tema claro/escuro/automático, mostrar/ocultar francês e inglês, vibrar ao notificar).
- Correção de condição de corrida: cliques repetidos na aba "Hoje" não geram mais leituras/escritas concorrentes no banco (acesso serializado por um `Mutex`), e a mesma aba não é recriada ao ser reclicada.

## Assinatura (debug.keystore)

O projeto já inclui `app/debug.keystore`, gerada com o alias/senha padrão do Android (`androiddebugkey` / `android`) e vinculada ao build de debug em `app/build.gradle`. Isso garante que todo APK gerado pelo GitHub Actions seja assinado sempre com a mesma chave — importante para conseguir instalar builds novos por cima de builds antigos sem erro de "assinaturas conflitantes".

**Atenção:** se você já tinha instalado uma versão anterior do app assinada com outra chave (ex.: gerada localmente pelo Android Studio), o Android vai recusar a instalação da nova versão por cima dela. Nesse caso, desinstale o app do celular antes de instalar o novo APK — depois disso, todas as atualizações futuras vindas do Actions vão instalar normalmente por cima, sem precisar desinstalar de novo.

## Sobre esta atualização (o esquema do banco mudou de novo)

Esta versão adicionou o campo "horário real do alerta" a cada palavra (para a Agenda nunca alterar retroativamente o horário exibido quando você muda a configuração). Com `fallbackToDestructiveMigration()` ativo, **seu histórico será apagado de novo** na primeira abertura desta versão.

## Novidades desta rodada

- **Botão "Reiniciar ciclo atual"** em Configurações → Preferências (com confirmação): volta a contagem dos 7 dias para o dia 1 a partir de hoje, sem apagar palavras já aprendidas.
- **Agenda em cards, da data mais antiga para a mais nova**, com uma faixa colorida indicando o status (verde = aprendida, cinza = ainda não aprendida, dourado = dia futuro, vermelho = dia sem palavra gerada).
- **Horário histórico por palavra**: cada entrada da Agenda mostra o horário que estava configurado no dia em que ela foi gerada — mudar o horário nas Configurações só afeta os próximos dias, nunca reescreve o passado.
- **Tema escuro de verdade**: ao escolher "Escuro" (ou "Automático" com o sistema em modo escuro), o fundo do app agora escurece de fato, com texto claro para manter a leitura confortável.
- **Tamanhos de fonte maiores**: Pequena/Média/Grande foram todas aumentadas (a Grande bem mais que antes).
- **64 novas palavras** um pouco mais acessíveis que as 511 originais, mas ainda fora do vocabulário do dia a dia — para variar o nível de dificuldade sem perder o objetivo de enriquecimento vocabular.

## Como publicar (mesmo fluxo do app "Notas Rápidas")

Como você não usa Android Studio localmente, o build é feito pelo GitHub Actions:

1. Crie um repositório novo no GitHub (ex.: `palavra-do-dia`).
2. Suba todos os arquivos desta pasta para o repositório (mantendo a estrutura).
3. Vá em **Actions** no GitHub — o workflow `Build APK` roda automaticamente a cada push na branch `main` (ou dispare manualmente em "Run workflow").
4. Quando o build terminar, baixe o artefato `palavra-do-dia-apk` — ele contém o `app-debug.apk`.
5. Transfira o APK para o celular e instale (permitindo "fontes desconhecidas" se pedido).

## Primeira abertura

1. O app explica o funcionamento e pede para você escolher o horário da notificação diária.
2. Ele também vai pedir permissão de notificação (Android 13+).
3. **Importante para a notificação funcionar de forma confiável**: desative a otimização de bateria para o app nas configurações do seu aparelho (em celulares Xiaomi, Samsung, etc. isso é especialmente necessário, já que esses fabricantes matam apps em segundo plano de forma agressiva).

## Estrutura do projeto

```
app/src/main/assets/words.json         → banco de 511 palavras
app/src/main/java/.../data/            → entidades e DAOs do Room (Word, Progress, Cycle)
app/src/main/java/.../repository/      → toda a lógica de ciclo, teste e revisão
app/src/main/java/.../scheduler/       → AlarmManager, notificação, boot receiver
app/src/main/java/.../ui/              → telas (Home, Onboarding, Histórico, Teste, Desempenho, Revisão mensal, Configurações)
```

## Ampliando o banco de palavras no futuro

Basta editar `app/src/main/assets/words.json` e adicionar novos objetos no mesmo formato:

```json
{
  "id": 512,
  "palavra": "nova_palavra",
  "classe": "adjetivo",
  "definicao": "...",
  "exemplo": "...",
  "frances": "...",
  "ingles": "..."
}
```

O `id` deve ser único e sequencial — o app sempre usa a palavra de menor `id` ainda não utilizada, então nada precisa ser feito no código.

## Limitações conhecidas (v1 / MVP)

- Sem backup automático: desinstalar o app apaga o histórico (dá para adicionar exportação/importação numa v2).
- Sem áudio de pronúncia (pode ser adicionado com o TTS nativo do Android).
- Distratores do teste são sorteados aleatoriamente entre as 511 palavras — em casos raros podem ser pouco desafiadores ou (mais raramente) parecidos demais; ajustável depois com uma lista de distratores por dificuldade.
