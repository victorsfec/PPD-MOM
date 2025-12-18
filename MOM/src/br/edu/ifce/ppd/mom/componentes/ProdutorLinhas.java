package br.edu.ifce.ppd.mom.componentes;

import br.edu.ifce.ppd.mom.infra.ConfiguracaoJMS;
import br.edu.ifce.ppd.mom.gui.PainelDashboard;

import javax.jms.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Componente responsável pela leitura do arquivo de texto e envio das linhas para a fila JMS.
 * Atua como o "Produtor" no modelo MOM.
 */
public class ProdutorLinhas implements Runnable {
    
    // Enumeração para definir a estratégia de leitura: apenas linhas pares ou ímpares.
    // Isso permite instanciar duas threads lendo o mesmo arquivo de forma particionada.
    public enum TipoLeitura { PARES, IMPARES }

    private final String caminhoArquivo;
    private final TipoLeitura tipo;
    private final PainelDashboard gui;

    public ProdutorLinhas(String caminhoArquivo, TipoLeitura tipo, PainelDashboard gui) {
        this.caminhoArquivo = caminhoArquivo;
        this.tipo = tipo;
        this.gui = gui;
    }

    @Override
    public void run() {
        gui.registrarLog("[Produtor] Iniciando leitura do arquivo (Modo: " + tipo + ")...");
        
        // Estabelece a conexão com o provedor de mensagens (ActiveMQ)
        try (Connection conexao = ConfiguracaoJMS.criarFabricaConexao().createConnection()) {
            conexao.start();
            
            // Criação da sessão - sem transação, com confirmação automática de recebimento
            Session sessao = conexao.createSession(false, Session.AUTO_ACKNOWLEDGE);
            
            // Define o destino como uma Fila, pois queremos que cada linha seja processada apenas uma vez
            Destination filaDestino = sessao.createQueue(ConfiguracaoJMS.NOME_FILA_LINHAS);
            MessageProducer produtor = sessao.createProducer(filaDestino);

            File arquivo = new File(caminhoArquivo);
            if (!arquivo.exists()) {
                gui.registrarLog("[Erro] O arquivo especificado não foi encontrado: " + caminhoArquivo);
                return;
            }

            // Inicia a leitura do arquivo linha a linha
            try (BufferedReader leitor = new BufferedReader(new FileReader(arquivo))) {
                String conteudoLinha;
                long contadorLinha = 1;

                while ((conteudoLinha = leitor.readLine()) != null) {
                    // Verificação de segurança para permitir a interrupção da thread
                    if (Thread.currentThread().isInterrupted()) break;

                    // Lógica para determinar se a linha atual deve ser processada por esta instância
                    boolean ehPar = (contadorLinha % 2 == 0);
                    boolean deveProcessar = (tipo == TipoLeitura.PARES && ehPar) || 
                                          (tipo == TipoLeitura.IMPARES && !ehPar);

                    if (deveProcessar) {
                        // Cria a mensagem de texto contendo o conteúdo da linha
                        TextMessage mensagem = sessao.createTextMessage(conteudoLinha);
                        
                        // Adiciona uma propriedade extra (metadado) para fins de rastreabilidade
                        mensagem.setIntProperty("linha", (int) contadorLinha);
                        
                        // Envia a mensagem para a fila no Broker
                        produtor.send(mensagem);
                    }
                    contadorLinha++;
                }
            }
            gui.registrarLog("[Produtor] Leitura " + tipo + " finalizada com sucesso.");

        } catch (JMSException e) {
            gui.registrarLog("[Erro] Falha na comunicação JMS no Produtor " + tipo + ": " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}