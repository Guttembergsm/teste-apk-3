# AutoChamada 2.0 — projeto completo

## Recursos
- Tela principal com botões CHAMAR e PARAR TENTATIVAS.
- Área administrativa com senha inicial `1234` para configurar o telefone e alterar a senha.
- Número salvo no aparelho.
- Serviço em primeiro plano com notificação e novas tentativas 3 segundos após uma chamada terminar.
- GitHub Actions incluído para compilar o APK.

## Como usar
1. Envie os arquivos para a raiz do repositório GitHub, preservando as pastas.
2. Faça commit na branch `main` e aguarde o workflow `Gerar APK Android (AutoChamada)`.
3. Baixe o artefato `AutoChamada-debug-apk` e extraia o APK.
4. Instale no aparelho, abra as configurações com a senha inicial `1234`, cadastre o número e opcionalmente altere a senha.
5. Toque em CHAMAR e conceda permissões de telefone e notificações.

## Limitações
- O projeto foi refeito, mas não foi compilado nem testado em aparelho físico nesta sessão.
- PARAR TENTATIVAS cancela novas chamadas automáticas, mas não encerra uma chamada já ativa.
- Android, fabricante e operadora podem restringir a discagem automática e o funcionamento em segundo plano. Se a chamada não iniciar, será necessário examinar os logs do dispositivo.
- Use apenas para números que você tem autorização para chamar e respeite as regras da operadora e a legislação aplicável.
