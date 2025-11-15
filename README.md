ECHO_RUN

Visão geral
- Jogo 2D de plataforma roguelike/RPG, desenvolvido em Java.
- Este repositório contém um esqueleto inicial com tela principal, botões de Jogar e Configurações, e um botão no canto superior direito para alternar Tela Cheia.

Requisitos
- JDK 17+ instalado (`java` e `javac` no PATH).
- Linux (testado), mas deve funcionar em outros SOs com Java instalado.

Como executar
1) Dar permissão de execução aos scripts (uma vez):
```
chmod +x scripts/*.sh
```
2) Compilar e executar:
```
./scripts/run.sh
```

Como limpar artefatos de build
```
./scripts/clean.sh
```

Estrutura do projeto
```
src/
	main/
		java/
			com/
				echorun/
					EchoRun.java          # Janela principal + toggle de Tela Cheia
				echorun/ui/
					MainMenuPanel.java    # Tela inicial com "Jogar" e "Configurações"
scripts/
	run.sh                        # Compila e executa
	clean.sh                      # Limpa a pasta out/
out/                            # Saída de compilação (gerada)
```

Próximos passos (sugestões)
- Implementar a cena do jogo e o loop (Java2D) após o botão "Jogar".
- Criar painel real de configurações (áudio, vídeo, controles).
- Adicionar assets (sprites, fontes) e sistema de recursos.
- Estruturar estados (menu, jogo, pausa) e roteamento de telas.
