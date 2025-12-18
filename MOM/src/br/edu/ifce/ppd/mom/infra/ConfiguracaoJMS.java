package br.edu.ifce.ppd.mom.infra;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.ConnectionFactory;

/**
 * Classe de configuração central para a infraestrutura JMS.
 * Responsável por armazenar as constantes de conexão e nomes dos destinos (Filas e Tópicos),
 * garantindo que todos os componentes do sistema utilizem os mesmos endereços e identificadores.
 */
public class ConfiguracaoJMS {
    
    // Endereço do Broker ActiveMQ.
    // O protocolo tcp:// indica uma conexão via rede, a porta 61616 é a porta padrão de comunicação do ActiveMQ.
    public static final String URL_BROKER = "tcp://localhost:61616";
    
    // Nome da Fila (Queue) utilizada para distribuir as linhas do arquivo entre os Workers.
    // O modelo de Fila garante o balanceamento de carga, onde cada mensagem é consumida por apenas um Worker.
    public static final String NOME_FILA_LINHAS = "MOM_FILA_LINHAS";
    
    // Nome do Tópico (Topic) utilizado para publicar os resultados.
    // O modelo de Tópico permite a subscrição de múltiplos interessados (Pub/Sub).
    public static final String NOME_TOPICO_RESULTADOS = "MOM_CONTADOR_PALAVRAS";

    /**
     * Cria e retorna a fábrica de conexões do ActiveMQ.
     * Este objeto Factory será utilizado pelos produtores e consumidores para estabelecer
     * a sessão de comunicação com o Broker.
     */
    public static ConnectionFactory criarFabricaConexao() {
        return new ActiveMQConnectionFactory(URL_BROKER);
    }
}