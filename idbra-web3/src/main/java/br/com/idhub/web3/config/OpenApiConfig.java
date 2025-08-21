package br.com.idhub.web3.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("🚀 IDBra Web3 Service API")
                                                .description("""
                                                                ## 🌟 **Serviço Centralizado de Blockchain Hyperledger Besu**

                                                                O **IDBra Web3 Service** é o coração da infraestrutura blockchain da IDBra, responsável por todas as interações com a rede Hyperledger Besu e smart contracts DID.

                                                                ### 🎯 **Arquitetura e Propósito**
                                                                Este microserviço centraliza toda a lógica de blockchain, permitindo que outros serviços (custody, issuer, holder, verifier) se comuniquem de forma padronizada com a rede distribuída.

                                                                ### 🔧 **Funcionalidades Principais**

                                                                #### **🔐 Gestão de Identidades (DID)**
                                                                - Criação e atualização de DIDs (Decentralized Identifiers)
                                                                - Verificação de existência e validação de identidades
                                                                - Gestão de documentos DID na blockchain

                                                                #### **👥 Controle de Acesso (RBAC)**
                                                                - Sistema de roles baseado em smart contracts
                                                                - Concessão e revogação de permissões
                                                                - Verificação de autorizações em tempo real

                                                                #### **📜 Gestão de Credenciais**
                                                                - Emissão e revogação de credenciais verificáveis
                                                                - Controle de status e validade
                                                                - Sistema de revogação distribuída

                                                                #### **🔗 Gestão de Delegados**
                                                                - Adição e remoção de representantes
                                                                - Controle de permissões temporárias
                                                                - Validação de autorizações

                                                                #### **📊 Monitoramento e Métricas**
                                                                - Status da conexão blockchain
                                                                - Métricas de operações e transações
                                                                - Health checks da infraestrutura

                                                                ### 🌐 **Endpoints Organizados por Categoria**

                                                                | Categoria | Endpoints | Descrição |
                                                                |-----------|-----------|-----------|
                                                                | **🔐 Roles** | `/api/blockchain/roles/*` | Gestão de permissões e autorizações |
                                                                | **🆔 DIDs** | `/api/blockchain/did/*` | Operações com identidades descentralizadas |
                                                                | **👥 Delegados** | `/api/blockchain/delegate/*` | Gestão de representantes |
                                                                | **📜 Credenciais** | `/api/blockchain/credentials/*` | Emissão e controle de credenciais |
                                                                | **🔍 KYC** | `/api/blockchain/kyc/*` | Verificação de identidade |
                                                                | **⚙️ Sistema** | `/api/blockchain/system/*` | Controle administrativo |
                                                                | **📊 Métricas** | `/api/blockchain/metrics` | Dados de monitoramento |

                                                                ### 🔐 **Autenticação e Segurança**

                                                                **ADMIN_PRIVATE_KEY**: Chave privada administrativa necessária para operações sensíveis.

                                                                ```bash
                                                                # Exemplo de configuração
                                                                ADMIN_PRIVATE_KEY=8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63
                                                                ```

                                                                ### 🚀 **Como Usar**

                                                                1. **Configure as variáveis de ambiente** (veja `.env.example`)
                                                                2. **Acesse o Swagger UI** em `/swagger-ui.html`
                                                                3. **Teste os endpoints** diretamente na interface
                                                                4. **Integre com outros serviços** via HTTP/REST

                                                                ### 📚 **Recursos Técnicos**

                                                                - **Blockchain**: Hyperledger Besu (Ethereum-compatible)
                                                                - **Smart Contracts**: Solidity com padrão ERC-1484
                                                                - **Web3j**: Cliente Java para Ethereum
                                                                - **Redis**: Cache para transações pendentes
                                                                - **Spring Boot 3.2**: Framework base

                                                                ### 🔗 **Integrações**

                                                                Este serviço é consumido por:
                                                                - **IDBra Custody Service** - Gestão de wallets
                                                                - **IDBra Issuer Service** - Emissão de credenciais
                                                                - **IDBra Holder Service** - Armazenamento de credenciais
                                                                - **IDBra Verifier Service** - Verificação de credenciais

                                                                ---

                                                                **📧 Suporte**: dev@idbra.com | **🌐 Website**: https://idbra.com
                                                                """)
                                                .version("2.0.0")
                                                .contact(new Contact()
                                                                .name("🚀 IDBra Development Team")
                                                                .email("dev@idbra.com")
                                                                .url("https://idbra.com"))
                                                .license(new License()
                                                                .name("MIT License")
                                                                .url("https://opensource.org/licenses/MIT")))
                                .servers(List.of(
                                                new Server()
                                                                .url("http://localhost:8081")
                                                                .description("🖥️ Servidor de Desenvolvimento Local"),
                                                new Server()
                                                                .url("https://dev-api.idbra.com")
                                                                .description("🧪 Servidor de Desenvolvimento"),
                                                new Server()
                                                                .url("https://staging-api.idbra.com")
                                                                .description("🔍 Servidor de Staging"),
                                                new Server()
                                                                .url("https://api.idbra.com")
                                                                .description("🚀 Servidor de Produção")))
                                .tags(List.of(
                                                new Tag().name("🔐 Role Management").description(
                                                                "Gestão de permissões e autorizações na blockchain"),
                                                new Tag().name("🆔 DID Management").description(
                                                                "Operações com Identidades Descentralizadas (DIDs)"),
                                                new Tag().name("👥 Delegate Management")
                                                                .description("Gestão de representantes e delegados"),
                                                new Tag().name("📜 Credential Management").description(
                                                                "Emissão, revogação e controle de credenciais"),
                                                new Tag().name("🔍 KYC Management").description(
                                                                "Verificação de identidade e Know Your Customer"),
                                                new Tag().name("⚙️ System Control")
                                                                .description("Controle administrativo e operacional"),
                                                new Tag().name("📊 Metrics & Monitoring")
                                                                .description("Métricas, status e monitoramento"),
                                                new Tag().name("🔗 Wallet Operations")
                                                                .description("Operações com carteiras blockchain"),
                                                new Tag().name("📝 Transaction Management")
                                                                .description("Gestão e monitoramento de transações")))
                                .components(new Components()
                                                .addSecuritySchemes("adminKey", new SecurityScheme()
                                                                .type(SecurityScheme.Type.APIKEY)
                                                                .in(SecurityScheme.In.HEADER)
                                                                .name("X-Admin-Key")
                                                                .description("Chave administrativa para operações sensíveis")))
                                .addSecurityItem(new SecurityRequirement().addList("adminKey"));
        }
}
