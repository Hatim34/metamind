/**
 * Photos libres de Wikimedia Commons. Les licences CC BY et CC BY-SA imposent de citer
 * l'auteur et la licence à côté de l'image : le crédit est donc toujours affiché.
 */
export interface Photo {
  src: string;
  credit: string;
  source: string;
}

function commons(src: string, author: string, licence: string, source: string): Photo {
  return { src, credit: `${author}, ${licence}, Wikimedia Commons`, source };
}

/** Clé : le nom de l'institution tel que l'API le renvoie. */
const INSTITUTION_PHOTOS: Record<string, Photo> = {
  'Université libre de Bruxelles': commons('/institutions/ULB.jpg', 'Jndemi', 'CC BY 3.0',
    'https://commons.wikimedia.org/wiki/File:Clock_Tower_of_the_ULB_Solbosch_Campus_in_the_City_of_Brussels.JPG'),
  'UCLouvain': commons('/institutions/UCL.jpg', 'EmDee', 'CC BY 4.0',
    'https://commons.wikimedia.org/wiki/File:Belgique_-_Louvain-la-Neuve_-_Biblioth%C3%A8que_des_Sciences_-_002.jpg'),
  'Université de Liège': commons('/institutions/ULG.jpg', 'Marc Ryckaert', 'CC BY 4.0',
    'https://commons.wikimedia.org/wiki/File:Li%C3%A8ge_Universit%C3%A9_Batiment_Central_R01.jpg'),
  'KU Leuven': commons('/institutions/KUL.jpg', 'Juhanson', 'CC BY-SA 3.0',
    'https://commons.wikimedia.org/wiki/File:Castle_Arenberg,_Katholieke_Universiteit_Leuven_adj.jpg'),
  'Universiteit Gent': commons('/institutions/UGE.jpg', 'ElienSmits', 'CC BY-SA 4.0',
    'https://commons.wikimedia.org/wiki/File:Aula_Gent.jpg'),
  'Vrije Universiteit Brussel': commons('/institutions/VUB.jpg', 'Romaine', 'CC0',
    'https://commons.wikimedia.org/wiki/File:Elsene-Rectoraatsgebouw_VUB_(1).jpg')
};

export const HOME_PHOTO: Photo = commons('/institutions/bibliotheque.jpg', 'Wentao Jiang', 'CC BY-SA 4.0',
  'https://commons.wikimedia.org/wiki/File:KU_Leuven_Library.jpg');

export function institutionPhoto(name: string): Photo | null {
  return INSTITUTION_PHOTOS[name] ?? null;
}
