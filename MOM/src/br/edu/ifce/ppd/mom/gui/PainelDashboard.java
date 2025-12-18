package br.edu.ifce.ppd.mom.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Interface Gráfica do Usuário para monitoramento do sistema MOM.
 * Esta classe atua como o painel de controle visual, exibindo logs de execução
 * em tempo real e as estatísticas consolidadas provenientes do Tópico.
 */
public class PainelDashboard extends JFrame {
    
    // Área de texto para logs: mostra o que cada thread está fazendo
    private JTextArea areaLogs;
    
    // Área de texto para estatísticas: mostra a contagem final das palavras
    private JTextArea areaEstatisticas;

    /**
     * Construtor: Configura o layout e os componentes visuais da janela.
     */
    public PainelDashboard() {
        setTitle("Dashboard - Projeto MOM (Monitoramento Distribuído)");
        setSize(800, 600);
        
        // Define que a janela será apenas fechada visualmente, permitindo que a aplicação principal controle o encerramento
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null); // Centraliza na tela
        
        // Divide a tela em 2 colunas: logs a esquerda, estatísticas a direita
        setLayout(new GridLayout(1, 2));

        // Configuração da área de Logs
        areaLogs = new JTextArea();
        areaLogs.setEditable(false); // O usuário não deve digitar aqui, apenas ler
        areaLogs.setBorder(BorderFactory.createTitledBorder("Logs do Sistema (Trace)"));
        
        // Configuração da área de Estatísticas
        areaEstatisticas = new JTextArea();
        areaEstatisticas.setEditable(false);
        areaEstatisticas.setBorder(BorderFactory.createTitledBorder("Resultados em Tempo Real (Subscriber)"));
        // Usa fonte para garantir que os números fiquem alinhados verticalmente
        areaEstatisticas.setFont(new Font("Monospaced", Font.BOLD, 14));

        // Adiciona barras de rolagem (scroll) caso o texto ultrapasse o tamanho da janela
        add(new JScrollPane(areaLogs));
        add(new JScrollPane(areaEstatisticas));
    }

    /**
     * Adiciona uma mensagem ao painel de logs.
     * * NOTA TÉCNICA: Como este método é chamado por threads externas (Workers/Produtores),
     * utilizamos SwingUtilities.invokeLater para garantir 'Thread Safety'.
     * Isso enfileira a atualização visual na Event Dispatch Thread (EDT) do Swing,
     * prevenindo condições de corrida e travamentos da interface.
     * * @param mensagem Texto a ser registrado no log.
     */
    public void registrarLog(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            areaLogs.append(mensagem + "\n");
            // Rola automaticamente para a última linha para mostrar a mensagem mais recente
            areaLogs.setCaretPosition(areaLogs.getDocument().getLength());
        });
    }

    /**
     * Atualiza o painel de estatísticas com o resumo completo.
     * Substitui todo o conteúdo atual pelo novo relatório gerado pelo Monitor.
     * * @param texto O relatório formatado contendo as contagens atuais.
     */
    public void atualizarEstatisticas(String texto) {
        SwingUtilities.invokeLater(() -> areaEstatisticas.setText(texto));
    }

    /**
     * Reseta a interface para uma nova execução.
     * Limpa os campos de texto e insere um marcador visual de início.
     */
    public void limparTela() {
        SwingUtilities.invokeLater(() -> {
            areaLogs.setText("");
            areaEstatisticas.setText("");
            registrarLog("=== PRONTO PARA NOVA BUSCA ===");
        });
    }
}