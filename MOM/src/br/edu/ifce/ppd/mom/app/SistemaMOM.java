package br.edu.ifce.ppd.mom.app;

import br.edu.ifce.ppd.mom.componentes.MonitorResultado;
import br.edu.ifce.ppd.mom.componentes.ProcessadorPalavras;
import br.edu.ifce.ppd.mom.componentes.ProdutorLinhas;
import br.edu.ifce.ppd.mom.gui.PainelDashboard;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Classe principal da aplicação (Cliente).
 * Responsável por instanciar a interface gráfica, gerenciar o ciclo de vida das Threads
 * (Produtores, Workers e Monitor) e orquestrar a execução do sistema.
 */
public class SistemaMOM extends JFrame {

    private JTextField campoPalavras;
    private JTextField campoArquivo;
    private JButton botaoIniciar;
    
    // Lista para manter referência das threads ativas, permitindo interrompê-las ao reiniciar
    private List<Thread> threadsAtivas = new ArrayList<>();
    private PainelDashboard dashboard; 

    public SistemaMOM() {
        setTitle("Sistema de Processamento Distribuído - MOM");
        setSize(500, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 1, 10, 10));

        JPanel p1 = new JPanel();
        p1.add(new JLabel("Caminho do Arquivo:"));
        campoArquivo = new JTextField("arquivo_teste.txt", 25);
        p1.add(campoArquivo);

        JPanel p2 = new JPanel();
        p2.add(new JLabel("Palavras-chave (csv):"));
        campoPalavras = new JTextField("Java,Python,ActiveMQ,MOM", 25);
        p2.add(campoPalavras);

        botaoIniciar = new JButton("Iniciar Processamento");
        botaoIniciar.addActionListener(e -> iniciarOuReiniciarSistema());

        add(p1);
        add(p2);
        add(botaoIniciar);
    }

    /**
     * Método acionado pelo botão da interface.
     * Realiza a limpeza do estado anterior e inicializa os componentes do sistema.
     */
    private void iniciarOuReiniciarSistema() {
        String textoArquivo = campoArquivo.getText();
        String textoPalavras = campoPalavras.getText();

        if (textoArquivo.isEmpty() || textoPalavras.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, preencha todos os campos obrigatórios.");
            return;
        }

        // Limpeza: Interrompe threads de execuções anteriores para evitar conflitos
        pararThreadsAntigas();

        // Interface: Prepara o painel de Dashboard se ainda não estiver visível
        if (dashboard == null || !dashboard.isVisible()) {
            dashboard = new PainelDashboard();
            dashboard.setVisible(true);
        }
        dashboard.limparTela(); 

        List<String> palavras = Arrays.asList(textoPalavras.split(","));

        //Inicialização do Subscriber
        // Inicia-se primeiro para garantir que nenhuma mensagem seja perdida
        Thread tSubscriber = new Thread(new MonitorResultado(dashboard, palavras));
        tSubscriber.start();
        threadsAtivas.add(tSubscriber);

        // Inicialização dos Workers
        // Cria-se 4 instâncias para simular o paralelismo no processamento
        for (int i = 1; i <= 4; i++) {
            Thread tWorker = new Thread(new ProcessadorPalavras(i, palavras, dashboard));
            tWorker.start();
            threadsAtivas.add(tWorker);
        }

        // Inicialização dos Produtores
        // Executado em uma thread separada com atraso para garantir que workers e monitores estejam prontos
        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            
            Thread leitorPar = new Thread(new ProdutorLinhas(textoArquivo, ProdutorLinhas.TipoLeitura.PARES, dashboard));
            Thread leitorImpar = new Thread(new ProdutorLinhas(textoArquivo, ProdutorLinhas.TipoLeitura.IMPARES, dashboard));
            
            leitorPar.start();
            leitorImpar.start();
            
            threadsAtivas.add(leitorPar);
            threadsAtivas.add(leitorImpar);
        }).start();

        botaoIniciar.setText("Reiniciar Processamento");
    }

    /**
     * Interrompe todas as threads listadas na coleção de threads ativas.
     */
    private void pararThreadsAntigas() {
        if (!threadsAtivas.isEmpty()) {
            System.out.println("Interrompendo " + threadsAtivas.size() + " threads ativas...");
            for (Thread t : threadsAtivas) {
                if (t.isAlive()) {
                    t.interrupt();
                }
            }
            threadsAtivas.clear();
            // Pequena pausa para permitir que o SO libere os recursos
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
    }

    public static void main(String[] args) {
        // Tenta ajustar o visual de acordo com o Sistema Operacional
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new SistemaMOM().setVisible(true));
    }
}