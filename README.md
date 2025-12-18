O projeto é um **Sistema de Contagem de Palavras Distribuído** que utiliza o padrão de mensagens **JMS (Java Message Service)** com o middleware **ActiveMQ**. O seu objetivo é processar um arquivo de texto grande em paralelo, dividindo o trabalho entre várias "threads".

### O Fluxo de Funcionamento

O sistema opera num ciclo de **Produtor-Consumidor** desacoplado:

1. **Leitura e Envio (Produtores):**
* O sistema inicia threads "Produtoras" (`ProdutorLinhas`).
* Estas threads leem o arquivo de texto linha por linha. Para aumentar a eficiência, uma thread lê as linhas pares e outra as ímpares.
* Cada linha lida é enviada imediatamente para uma **Fila (Queue)** chamada `MOM_FILA_LINHAS` no ActiveMQ.


2. **Processamento (Consumidores/Workers):**
* Várias threads "Trabalhadoras" (`ProcessadorPalavras`) ficam à escuta nessa Fila.
* Assim que uma linha chega à fila, um trabalhador livre retira-a e processa-a.
* O trabalhador conta quantas vezes as palavras-chave (ex: "Java", "Python") aparecem nessa linha.
* Em vez de guardar o resultado, o trabalhador **publica** o resultado num **Tópico (Topic)** chamado `MOM_CONTADOR_PALAVRAS`.


3. **Monitorização e Exibição (Subscriber):**
* Existe um componente "Monitor" (`MonitorResultado`) que assina (escuta) o Tópico de resultados.
* Sempre que um trabalhador publica uma contagem, o Monitor recebe essa mensagem em tempo real.
* Ele soma os valores num contador global e atualiza a interface gráfica (`PainelDashboard`) para mostrar o total de palavras encontradas até ao momento.


### Resumo da Arquitetura

* **Middleware:** O **Apache ActiveMQ** funciona como o correio central, garantindo que as mensagens (linhas do texto e resultados da contagem) circulam entre as partes do sistema sem que elas precisem de se conhecer diretamente.
* **Vantagem:** Se o ficheiro for muito grande, o sistema não trava, pois o trabalho é dividido e processado em paralelo por vários consumidores ao mesmo tempo.