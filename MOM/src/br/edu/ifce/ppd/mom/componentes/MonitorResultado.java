package br.edu.ifce.ppd.mom.componentes;

import br.edu.ifce.ppd.mom.infra.ConfiguracaoJMS;
import br.edu.ifce.ppd.mom.gui.PainelDashboard;

import javax.jms.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Componente "Subscriber" (Assinante).
 * Responsável por escutar o Tópico de resultados e agregar as estatísticas em tempo real
 * para exibição na interface gráfica.
 */
public class MonitorResultado implements Runnable {
    private final PainelDashboard gui;
    
    // Utiliza ConcurrentHashMap para garantir thread-safety, pois as mensagens chegam assincronamente
    private final Map<String, Integer> contadorGlobal = new ConcurrentHashMap<>();
    private Connection conexao;

    public MonitorResultado(PainelDashboard gui, List<String> palavrasIniciais) {
        this.gui = gui;
        // Inicializa o mapa de contagem com zero para todas as palavras solicitadas
        for(String p : palavrasIniciais) {
            contadorGlobal.put(p.trim(), 0);
        }
        atualizarTela();
    }

    @Override
    public void run() {
        gui.registrarLog("[Subscriber] Monitor de resultados ativo e aguardando dados...");
        try {
            conexao = ConfiguracaoJMS.criarFabricaConexao().createConnection();
            conexao.start();
            Session sessao = conexao.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Conecta-se ao Tópico para receber as atualizações dos Workers
            Destination topico = sessao.createTopic(ConfiguracaoJMS.NOME_TOPICO_RESULTADOS);
            MessageConsumer assinante = sessao.createConsumer(topico);

            // Listener que processa cada mensagem de resultado recebida
            assinante.setMessageListener(msg -> {
                if (msg instanceof MapMessage) {
                    try {
                        MapMessage map = (MapMessage) msg;
                        String termo = map.getString("termo");
                        int qtd = map.getInt("ocorrencias");
                        int workerId = map.getInt("origemWorkerId");

                        // Atualiza o contador global de forma atômica (soma o valor atual com o novo)
                        contadorGlobal.merge(termo, qtd, Integer::sum);
                        
                        // Registra log visual da operação
                        gui.registrarLog("[Subscriber] Worker " + workerId + " notificou: " + termo + " (+" + qtd + ")");
                        
                        // Solicita atualização da interface visual
                        atualizarTela();
                    } catch (JMSException e) { e.printStackTrace(); }
                }
            });

            // Mantém o monitor ativo aguardando notificações
            synchronized (this) {
                wait();
            }

        } catch (InterruptedException e) {
            gui.registrarLog("[Subscriber] Processo de monitoramento interrompido.");
        } catch (Exception e) {
            gui.registrarLog("[Subscriber] Erro interno: " + e.getMessage());
        } finally {
            try { if (conexao != null) conexao.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Formata os dados acumulados e atualiza o painel de estatísticas da GUI.
     * ALTERAÇÃO REALIZADA: Adicionado cálculo e exibição do TOTAL GERAL.
     */
    private void atualizarTela() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ESTATÍSTICAS EM TEMPO REAL ===\n\n");
        
        // Itera sobre o mapa e formata a saída para cada palavra
        contadorGlobal.forEach((k, v) -> sb.append(String.format("%-15s : %d\n", k, v)));

        // --- CÁLCULO DA SOMA TOTAL ---
        // Utiliza Stream API para somar todos os valores (inteiros) presentes no mapa
        int totalGeral = contadorGlobal.values().stream()
                                       .mapToInt(Integer::intValue)
                                       .sum();

        // Adiciona uma linha divisória e o total formatado
        sb.append("\n------------------------------\n");
        sb.append(String.format("%-15s : %d", "TOTAL GERAL", totalGeral));

        // Envia o texto completo para o Dashboard
        gui.atualizarEstatisticas(sb.toString());
    }
}