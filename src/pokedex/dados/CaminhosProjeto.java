package pokedex.dados;

import java.nio.file.Files;
import java.nio.file.Path;

/** Localiza os recursos ao executar pela pasta do projeto ou pela pasta acima. */
public final class CaminhosProjeto {
    private CaminhosProjeto() {}

    public static Path resolver(String caminho) {
        Path solicitado = Path.of(caminho);
        if (solicitado.isAbsolute()) {
            return solicitado;
        }
        Path atual = Path.of("").toAbsolutePath();
        if (Files.isRegularFile(atual.resolve("pokemons.csv"))) {
            return atual.resolve(solicitado);
        }
        Path projeto = atual.resolve("pokedex-abb");
        if (Files.isRegularFile(projeto.resolve("pokemons.csv"))) {
            return projeto.resolve(solicitado);
        }
        return solicitado;
    }
}
