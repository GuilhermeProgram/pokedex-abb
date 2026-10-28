package pokedex.busca;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import pokedex.abb.ArvoreBinaria;
import pokedex.abb.No;
import pokedex.modelo.Pokemon;

import static pokedex.ui.tema.Tema.*;

public class BuscaPokemon {

    // Busca o Pokémon pelo número
    public static Pokemon buscarPorNumero(
            ArvoreBinaria arvore, int numero) {

        if (arvore == null) {
            return null;
        }

        return arvore.buscar(numero);
    }

    // Busca o Pokémon pelo nome (nome inteiro, sem diferenciar maiúsculas nem acentos)
    public static Pokemon buscarPorNome(
            ArvoreBinaria arvore, String nome) {

        if (arvore == null || nome == null) {
            return null;
        }

        return buscarNomeNo(arvore.getRaiz(), normalizar(nome));
    }

    // Vai passando pelos nós até achar o nome
    private static Pokemon buscarNomeNo(
            No atual, String nome) {

        if (atual == null) {
            return null;
        }

        // Confere se é o Pokémon que estamos procurando
        if (normalizar(atual.getPokemon().getNome()).equals(nome)) {
            return atual.getPokemon();
        }

        // Procura primeiro do lado esquerdo
        Pokemon encontrado = buscarNomeNo(
                atual.getEsquerda(), nome);

        if (encontrado != null) {
            return encontrado;
        }

        // Se não achou, procura do lado direito
        return buscarNomeNo(atual.getDireita(), nome);
    }

    // Busca todos os Pokémon cujo nome começa com o texto (ex.: "char" acha Charmander,
    // Charmeleon e Charizard). O resultado vem em ordem crescente do número da Pokédex.
    public static List<Pokemon> buscarPorPrefixo(
            ArvoreBinaria arvore, String texto) {

        List<Pokemon> resultado = new ArrayList<>();

        if (arvore == null || texto == null) {
            return resultado;
        }

        String prefixo = normalizar(texto);

        if (prefixo.isEmpty()) {
            return resultado;
        }

        prefixoNo(arvore.getRaiz(), prefixo, resultado);
        return resultado;
    }

    // Percurso em ordem: esquerda, nó, direita (por isso o resultado sai ordenado por Dex)
    private static void prefixoNo(
            No atual, String prefixo, List<Pokemon> resultado) {

        if (atual == null) {
            return;
        }

        prefixoNo(atual.getEsquerda(), prefixo, resultado);

        if (normalizar(atual.getPokemon().getNome()).startsWith(prefixo)) {
            resultado.add(atual.getPokemon());
        }

        prefixoNo(atual.getDireita(), prefixo, resultado);
    }

    // Busca usada pela interface: número, depois nome inteiro, depois o começo do nome
    public static Pokemon buscar(
            ArvoreBinaria arvore, String texto) {

        if (arvore == null || texto == null) {
            return null;
        }

        String busca = texto.trim();

        if (busca.isEmpty()) {
            return null;
        }

        // Só dígitos: busca pelo número na ABB
        if (busca.matches("\\d+")) {
            try {
                return buscarPorNumero(arvore, Integer.parseInt(busca));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        Pokemon exato = buscarPorNome(arvore, busca);

        if (exato != null) {
            return exato;
        }

        List<Pokemon> comecam = buscarPorPrefixo(arvore, busca);
        return comecam.isEmpty() ? null : comecam.get(0);
    }

    // Tira acentos e maiúsculas para comparar nomes ("Flabébé" = "flabebe")
    static String normalizar(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
