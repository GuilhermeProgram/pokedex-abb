# -*- coding: utf-8 -*-
"""
Gera dados/golpes.csv (catalogo de golpes com PP e efeitos) e dados/golpes_pokemon.csv (os 4 golpes de cada Pokemon).

Fonte dos dados: Smogon Strategy Dex, formato "Champions" (https://www.smogon.com/dex/champions/pokemon/)
  - ferramentas/smogon/golpes_smogon.txt   golpes com poder, precisao, prioridade, PP e efeito (ja convertido para os
                                           codigos que a batalha entende)
  - ferramentas/smogon/pokemon_smogon.txt  #SETS: golpes recomendados pelo Smogon (quando existem)
                                           #LEARN: quais golpes cada Pokemon realmente pode aprender (bitmap em base64)

Uso (na raiz do projeto):  python ferramentas/gerar_golpes.py

Regra dos 4 golpes de cada Pokemon:
  1. se o Smogon recomenda um conjunto, usa esses golpes (os que a batalha consegue executar);
  2. o que faltar e completado por regra: STAB (golpe do tipo do Pokemon) na categoria em que ele e mais forte
     (fisico ou especial), um golpe de cobertura (tipo que acerta super efetivo onde o STAB nao acerta) e um golpe de
     utilidade conforme o papel (rapido e forte -> aumenta ataque; resistente -> cura ou status; senao -> enfraquecer);
  3. so entram golpes que o Pokemon aprende (lista do Smogon). Pokemon fora do formato Champions usam o catalogo todo.
A "sorte" usada para variar e sempre a mesma (vem do numero da Pokedex), entao rodar de novo gera o mesmo resultado.
"""
import csv, os, random, base64

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
D = os.path.join(RAIZ, 'dados')
S = os.path.join(RAIZ, 'ferramentas', 'smogon')

TIPO_PT = {'Fire':'Fogo','Water':'Água','Grass':'Planta','Electric':'Elétrico','Ice':'Gelo','Fighting':'Lutador',
           'Poison':'Veneno','Ground':'Terra','Flying':'Voador','Psychic':'Psíquico','Bug':'Inseto','Rock':'Pedra',
           'Ghost':'Fantasma','Dragon':'Dragão','Dark':'Sombrio','Steel':'Aço','Fairy':'Fada','Normal':'Normal'}
CAT_PT = {'F':'Físico','E':'Especial','S':'Status'}

# ---------------------------------------------------------------- 1) catálogo
catalogo = []
with open(os.path.join(S, 'golpes_smogon.txt'), encoding='utf-8') as f:
    for i, linha in enumerate(f):
        if not linha.strip():
            continue
        nome, tipo, cat, poder, prec, prio, pp, ef, ch, al = linha.rstrip('\n').split('|')
        catalogo.append(dict(idx=i, nome=nome, tipo=TIPO_PT[tipo], cat=CAT_PT[cat], poder=int(poder), prec=int(prec),
                             prio=int(prio), pp=int(pp), efeito=ef, chance=int(ch), alvo=al))
for n, g in enumerate(catalogo, start=1):
    g['id'] = n
with open(os.path.join(D, 'golpes.csv'), 'w', encoding='utf-8', newline='') as f:
    w = csv.writer(f, delimiter=';', lineterminator='\n')
    w.writerow(['id','nome','tipo','categoria','poder','precisao','prioridade','pp','efeito','chance','alvo'])
    for g in catalogo:
        w.writerow([g['id'], g['nome'], g['tipo'], g['cat'], g['poder'], g['prec'], g['prio'], g['pp'],
                    g['efeito'], g['chance'], g['alvo']])
G = {g['nome']: g for g in catalogo}

# ---------------------------------------------------------------- 2) dados do Smogon por Pokémon
sets_smogon, aprende = {}, {}
secao = None
with open(os.path.join(S, 'pokemon_smogon.txt'), encoding='utf-8') as f:
    for linha in f:
        linha = linha.rstrip('\n')
        if linha.startswith('#'):
            secao = linha
            continue
        if not linha:
            continue
        partes = linha.split('|')
        if secao == '#SETS':
            sets_smogon[partes[0]] = [m for m in partes[1:] if m in G]
        else:
            B64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
            bits = ''.join(format(B64.index(c), '06b') for c in partes[1])
            aprende[partes[0]] = {catalogo[i]['nome'] for i, b in enumerate(bits) if b == '1' and i < len(catalogo)}

def ler(nome):
    with open(os.path.join(D, nome), encoding='utf-8') as f:
        return list(csv.DictReader(f, delimiter=';'))

pokes = {int(r['numeroDex']): r for r in ler('../pokemons.csv')}
attrs = {int(r['numeroDex']): {k: int(v) for k, v in r.items()} for r in ler('atributos.csv')}
vant = {}
with open(os.path.join(D, 'vantagens.csv'), encoding='utf-8') as f:
    for l in list(csv.reader(f, delimiter=';'))[1:]:
        vant[(l[0], l[1])] = int(l[2]) / 100.0
TIPOS = sorted({g['tipo'] for g in catalogo})

def mult(tipo_golpe, tipos_def):
    m = 1.0
    for t in tipos_def:
        m *= vant.get((tipo_golpe, t), 1.0)
    return m

# golpes com regra especial nos jogos (que esta batalha não tem): ficam no catálogo mas não são sorteados
EXCLUIDOS = {'Burn Up','Stored Power','Round','Terrain Pulse','Weather Ball','Steel Roller','Gigaton Hammer','Thief','Payback',
             'Assurance','Stomping Tantrum','Facade','Body Press','Foul Play','Eruption','Water Spout','Last Resort',
             'Focus Punch','Fake Out','Feint','Upper Hand','Power Trip','Snore','Struggle','Beak Blast','Circle Throw',
             'Dragon Tail','Avalanche','Pound','Bind','Wrap','Whirlpool','Sand Tomb','Fire Spin','Infestation','Rapid Spin',
             'Uproar','Mud-Slap','Mortal Spin','Salt Cure','Snap Trap','Last Respects','Rage Fist','Infernal Parade'}
ataques = [g for g in catalogo if g['cat'] != 'Status' and g['nome'] not in EXCLUIDOS]

def valor(g):  # "qualidade" do golpe: poder esperado, com pequena penalidade a efeitos ruins
    v = min(g['poder'], 130) * g['prec'] / 100.0
    if g['efeito'] == 'RECOIL': v *= 0.85
    if g['alvo'] == 'eu' and any(x[3] == '-' for x in g['efeito'].split(',') if len(x) > 3): v *= 0.85
    if g['prio'] > 0: v *= 0.9
    return v

def melhores(legais, tipo, cat_pref, excluir, so_prioridade=False, min_poder=0):
    c = [g for g in ataques if g['tipo'] == tipo and g['nome'] in legais and g['nome'] not in excluir
         and g['poder'] >= min_poder and (g['prio'] > 0) == so_prioridade]
    c.sort(key=lambda g: (g['cat'] == cat_pref, valor(g)), reverse=True)
    return c

def escolher(lista, rnd, topo=3):
    """pega um dos 'topo' melhores (varia entre Pokémon parecidos, mas sempre entre os bons)"""
    return lista[rnd.randrange(min(topo, len(lista)))] if lista else None

CURAS = ['Recover', 'Slack Off', 'Soft-Boiled', 'Milk Drink', 'Roost', 'Synthesis', 'Moonlight', 'Morning Sun']

def preferencias_utilidade(dex, tipos, a, fisico, rnd):
    ofensa = max(a['ataque'], a['ataqueEspecial'])
    vel = a['velocidade']
    resist = a['hp'] + a['defesa'] + a['defesaEspecial']
    if ofensa >= 90 and vel >= 80 and rnd.random() < 0.8:
        if fisico:
            p = ['Dragon Dance'] if 'Dragão' in tipos else ['Bulk Up', 'Swords Dance'] if 'Lutador' in tipos else ['Swords Dance']
        else:
            p = ['Quiver Dance'] if 'Inseto' in tipos else ['Calm Mind', 'Nasty Plot'] if 'Psíquico' in tipos else ['Nasty Plot']
        return p + ['Shell Smash', 'Agility', 'Rock Polish']
    if resist >= 230 and rnd.random() < 0.7:
        c = CURAS[:]
        rnd.shuffle(c)
        if 'Planta' in tipos: c.insert(0, 'Synthesis')
        if 'Voador' in tipos: c.insert(0, 'Roost')
        if 'Fada' in tipos: c.insert(0, 'Moonlight')
        return c + ['Toxic', 'Will-O-Wisp', 'Iron Defense', 'Cosmic Power', 'Amnesia']
    if vel >= 90:
        p = ['Thunder Wave', 'Glare', 'Will-O-Wisp']
        rnd.shuffle(p)
        return p + ['Toxic', 'Agility']
    p = ['Toxic', 'Will-O-Wisp', 'Thunder Wave', 'Hypnosis', 'Screech', 'Charm', 'Fake Tears', 'Scary Face', 'Sing', 'Tickle']
    rnd.shuffle(p)
    if 'Planta' in tipos: p = ['Sleep Powder', 'Stun Spore', 'Spore', 'Poison Powder'] + p
    if 'Veneno' in tipos: p.insert(0, 'Toxic')
    if 'Inseto' in tipos: p.insert(0, 'String Shot')
    if 'Fada' in tipos: p.insert(0, 'Charm')
    if 'Aço' in tipos: p.insert(0, 'Metal Sound')
    return p

def montar(dex, nome, tipos):
    a = attrs[dex]
    rnd = random.Random(dex * 7919)
    fisico = a['ataque'] >= a['ataqueEspecial']
    pref = 'Físico' if fisico else 'Especial'
    vel = a['velocidade']
    legais = aprende.get(nome)
    origem = 'regra'
    if legais is None:                       # fora do formato Champions: catálogo todo
        legais = {g['nome'] for g in catalogo}
    usados = []
    if nome in sets_smogon:                  # 1) conjunto recomendado pelo Smogon
        usados = [m for m in sets_smogon[nome] if m in legais][:4]
        if usados:
            origem = 'smogon'
    def pegar(g):
        if g and g['nome'] not in usados and len(usados) < 4:
            usados.append(g['nome'])
            return True
        return False

    if len(usados) < 4 and not any(G[m]['tipo'] == tipos[0] and G[m]['cat'] != 'Status' for m in usados):
        pegar(escolher(melhores(legais, tipos[0], pref, usados, min_poder=60) or melhores(legais, tipos[0], pref, usados), rnd))
    if len(usados) < 4 and len(tipos) > 1 and not any(G[m]['tipo'] == tipos[1] and G[m]['cat'] != 'Status' for m in usados):
        pegar(escolher(melhores(legais, tipos[1], pref, usados, min_poder=60) or melhores(legais, tipos[1], pref, usados), rnd))
    elif len(usados) < 4 and len(tipos) == 1 and vel >= 60 and rnd.random() < 0.25:
        pegar(escolher(melhores(legais, tipos[0], pref, usados, so_prioridade=True), rnd, 1))
    if len(usados) < 4 and sum(1 for m in usados if G[m]['cat'] != 'Status') < 2:
        pegar(escolher(melhores(legais, tipos[0], pref, usados, min_poder=50) or melhores(legais, tipos[0], pref, usados), rnd))
    # cobertura
    if len(usados) < 4 and sum(1 for m in usados if G[m]['cat'] != 'Status') < 3:
        stab_tipos = {G[n]['tipo'] for n in usados if G[n]['cat'] != 'Status'} or set(tipos)
        ganho = []
        for t in TIPOS:
            if t in stab_tipos:
                continue
            pegos = sum(1 for d in TIPOS if all(mult(s, [d]) <= 1 for s in stab_tipos) and mult(t, [d]) > 1)
            ganho.append((pegos + rnd.random() * 0.5, t))
        ganho.sort(reverse=True)
        for _, t in ganho:
            c = melhores(legais, t, pref, usados, min_poder=70)
            if c and pegar(escolher(c, rnd, 2)):
                break
    # utilidade
    if len(usados) < 4 and not any(G[m]['cat'] == 'Status' for m in usados):
        for nm in preferencias_utilidade(dex, tipos, a, fisico, rnd):
            if nm in legais and pegar(G[nm]):
                break
    # completa com o melhor ataque que sobrar
    resto = []
    for t in tipos + TIPOS:
        resto += melhores(legais, t, pref, usados, min_poder=40)
    for g in resto:
        pegar(g)
    if len(usados) < 4:                       # Pokémon que quase não aprende nada: qualquer ataque do catálogo
        for g in sorted(ataques, key=valor, reverse=True):
            pegar(g)
    return usados[:4], origem

total = {'smogon': 0, 'regra': 0}
with open(os.path.join(D, 'golpes_pokemon.csv'), 'w', encoding='utf-8', newline='') as f:
    w = csv.writer(f, delimiter=';', lineterminator='\n')
    w.writerow(['numeroDex', 'golpe1', 'golpe2', 'golpe3', 'golpe4'])
    for dex in sorted(attrs):
        p = pokes[dex]
        tipos = [p['tipo1']] + ([p['tipo2']] if p['tipo2'] else [])
        golpes, origem = montar(dex, p['nome'], tipos)
        total[origem] += 1
        w.writerow([dex] + [G[n]['id'] for n in golpes] + [''] * (4 - len(golpes)))
print('golpes no catalogo:', len(catalogo), '| pokemon:', len(attrs), '| com conjunto do Smogon:', total['smogon'])
