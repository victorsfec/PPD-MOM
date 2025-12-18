package br.edu.ifce.ppd.mom.componentes;

import br.edu.ifce.ppd.mom.infra.ConfiguracaoJMS;
import br.edu.ifce.ppd.mom.gui.PainelDashboard;

import javax.jms.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Componente Worker responsável pelo processamento dos dados.
 * Ele consome linhas da Fila, conta as palavras-chave e publica os resultados em um Tópico.
 */
public class ProcessadorPalavras implements Runnable {
    private final int idWorker;
    private final List<String> palavrasAlvo;
    private final PainelDashboard gui;
    private Connection conexao;

    public ProcessadorPalavras(int id, List<String> palavrasAlvo, PainelDashboard gui) {
        this.idWorker = id;
        this.palavrasAlvo = palavrasAlvo;
        this.gui = gui;
    }

    @Override
    public void run() {
        gui.registrarLog("[Worker " + idWorker + "] Serviço iniciado. Aguardando mensagens...");
        try {
            // Configura a conexão com o middleware de mensageria
            conexao = ConfiguracaoJMS.criarFabricaConexao().createConnection();
            conexao.start();
            Session sessao = conexao.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Consumidor: Conecta-se à Fila para receber as linhas de texto pendentes
            Destination filaOrigem = sessao.createQueue(ConfiguracaoJMS.NOME_FILA_LINHAS);
            MessageConsumer consumidor = sessao.createConsumer(filaOrigem);

            // Produtor: Conecta-se ao Tópico para publicar as estatísticas encontradas
            Destination topicoDestino = sessao.createTopic(ConfiguracaoJMS.NOME_TOPICO_RESULTADOS);
            MessageProducer publicador = sessao.createProducer(topicoDestino);

            // Configura um Listener assíncrono para processar mensagens assim que chegarem
            consumidor.setMessageListener(msg -> {
                try {
                    if (msg instanceof TextMessage) {
                        String textoLinha = ((TextMessage) msg).getText();
                        // Delega o processamento da linha para o método auxiliar
                        processarLinha(textoLinha, sessao, publicador);
                    }
                } catch (JMSException e) {
                    // Exceções de conexão podem ocorrer no encerramento, são ignoradas aqui
                }
            });

            // Mantém a thread em estado de espera (bloqueada) para que o Listener continue ativo
            synchronized (this) {
                wait();
            }

        } catch (InterruptedException e) {
            gui.registrarLog("[Worker " + idWorker + "] Encerrando execução a pedido do usuário...");
        } catch (JMSException e) {
            gui.registrarLog("[Worker " + idWorker + "] Falha na conexão JMS: " + e.getMessage());
        } finally {
            // Garante o fechamento adequado dos recursos de rede
            try { if (conexao != null) conexao.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Analisa o texto recebido e envia notificações para cada palavra encontrada.
     * * NOTA TÉCNICA: Para garantir consistência entre o monitoramento do ActiveMQ (Messages Enqueued)
     * e o Dashboard da aplicação, este método envia uma mensagem individual para CADA ocorrência.
     * Exemplo: Se "Java" aparece 3 vezes, enviam-se 3 mensagens contendo valor 1.
     */
    private void processarLinha(String linha, Session sessao, MessageProducer publicador) throws JMSException {
        for (String palavraChave : palavrasAlvo) {
            int ocorrencias = contarNoTexto(linha, palavraChave);
            
            // Itera sobre o número total de ocorrências encontradas na linha
            for (int i = 0; i < ocorrencias; i++) {
                // Cria uma mensagem do tipo Map para estruturar os dados (chave-valor)
                MapMessage mapaResultados = sessao.createMapMessage();
                
                mapaResultados.setString("termo", palavraChave);
                // Define valor unitário para que o contador de mensagens do Broker reflita o total real
                mapaResultados.setInt("ocorrencias", 1); 
                mapaResultados.setInt("origemWorkerId", idWorker);
                
                // Publica a mensagem no Tópico de resultados
                publicador.send(mapaResultados);
            }
        }
    }

    /**
     * Realiza a contagem de ocorrências utilizando Expressões Regulares (Regex).
     * Utiliza limites de palavra (\b) para evitar falsos positivos em substrings.
     */
    private int contarNoTexto(String texto, String alvo) {
        if (texto == null || alvo == null) return 0;
        int count = 0;
        // Compila o padrão regex ignorando maiúsculas/minúsculas
        Pattern p = Pattern.compile("\\b" + Pattern.quote(alvo) + "\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        while (m.find()) count++;
        return count;
    }
}