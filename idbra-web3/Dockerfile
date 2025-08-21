FROM openjdk:11-jre-slim

# Criar usuário não-root
RUN groupadd -r custody && useradd -r -g custody custody

# Definir diretório de trabalho
WORKDIR /app

# Copiar o JAR da aplicação
COPY target/custody-service-*.jar app.jar

# Mudar propriedade do arquivo para o usuário custody
RUN chown custody:custody app.jar

# Mudar para usuário não-root
USER custody

# Expor porta
EXPOSE 8080

# Variáveis de ambiente padrão
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV BESU_NODE_URL="http://localhost:8545"
ENV BESU_CHAIN_ID="1337"

# Comando para executar a aplicação
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
