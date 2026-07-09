package com.tn.gias.transport.service;

import com.tn.gias.transport.dto.DemandeRequest;
import com.tn.gias.transport.entity.*;
import com.tn.gias.transport.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.LocalDateTime;

import java.io.FileWriter;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;
@Service
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final SiteRepository siteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FournisseurRepository fournisseurRepository;
    private final CamionRepository camionRepository;
    private final ArticleRepository articleRepository;
    private final EmailService emailService;

    public DemandeService(DemandeRepository demandeRepository,
                          SiteRepository siteRepository,
                          UtilisateurRepository utilisateurRepository,
                          FournisseurRepository fournisseurRepository,
                          CamionRepository camionRepository,
                          ArticleRepository articleRepository,
                          EmailService emailService) {
        this.demandeRepository = demandeRepository;
        this.siteRepository = siteRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.camionRepository = camionRepository;
        this.articleRepository = articleRepository;
        this.emailService = emailService;
    }

    // 🔥 CREATE DEMANDE
    public Demande save(DemandeRequest request) {

        Demande d = new Demande();

        d.setLibelle(request.getLibelle());
        d.setCapacite(request.getCapacite());
        d.setDateValidation(LocalDateTime.now());
        d.setStatut(StatutDemande.EN_ATTENTE);
        d.setDateDemande(LocalDateTime.now());
        d.setStatut(StatutDemande.EN_ATTENTE);
        d.setSiteDepart(siteRepository.findById(request.getSiteDepartId()).orElseThrow());
        d.setSiteArrivee(siteRepository.findById(request.getSiteArriveeId()).orElseThrow());

        // 🔐 USER CONNECTÉ
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        d.setDemandeur(user);

        d.setFournisseur(fournisseurRepository.findById(request.getFournisseurId()).orElseThrow());
        d.setCamion(camionRepository.findById(request.getCamionId()).orElseThrow());

        List<LigneDemande> lignes = request.getLignes().stream().map(l -> {
            LigneDemande ld = new LigneDemande();

            ld.setArticle(articleRepository.findById(l.getArticleId()).orElseThrow());
            ld.setQuantite(l.getQuantite());
            ld.setUnite(l.getUnite());

            // 🔥 NOUVEAU
            ld.setType(TypeMouvement.valueOf(l.getType()));
            ld.setDescription(l.getDescription());

            ld.setDemande(d);

            return ld;
        }).toList();

        d.setLignes(lignes);

        return demandeRepository.save(d);
    }

    // 🔥 VALIDER
    @Transactional
    public Demande valider(Long id) {

        // 🔍 نجيب demande
        Demande d = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        // ❌ déjà traitée
        if (d.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new RuntimeException("Demande déjà traitée");
        }

        // 🔍 vérifier règles métier
        String erreur = verifierDemande(d);

        if (erreur != null) {
            return refuser(id, erreur); // 🔥 refus automatique
        }

        // 🔥 1. تحديث STOCK
        for (LigneDemande l : d.getLignes()) {

            Article article = l.getArticle();

            if (l.getType() == TypeMouvement.SORTIE) {
                article.setQuantite(article.getQuantite() - l.getQuantite());
            }
            else if (l.getType() == TypeMouvement.ENTREE) {
                article.setQuantite(article.getQuantite() + l.getQuantite());
            }

            articleRepository.save(article);
        }

        // 🔥 2. validation
        d.setStatut(StatutDemande.VALIDE);
        d.setDateValidation(LocalDateTime.now());

        // 📧 email
        emailService.sendEmail(
                d.getDemandeur().getEmail(),
                "Demande validée",
                "Votre demande N°" + d.getId() + " a été validée"
        );

        // 📄 CSV
        genererCSV(d);

        return demandeRepository.save(d);

    }
    public File telechargerCsv(Long id) {

        Demande d = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        return genererCSV(d);
    }

    // 🔥 REFUSER
    public Demande refuser(Long id, String motif) {

        Demande d = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (d.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new RuntimeException("Demande déjà traitée");
        }

        d.setStatut(StatutDemande.REJETE);
        d.setDateValidation(LocalDateTime.now());

        // 🔥 EMAIL AVEC MOTIF
        emailService.sendEmail(
                d.getDemandeur().getEmail(),
                "Demande refusée",
                "Votre demande a été refusée.\nMotif : " + motif
        );

        return demandeRepository.save(d);
    }


    // 🔥 CSV

    private File genererCSV(Demande d) {

        try {

            String nomFichier = "TRG_" + d.getId() + ".csv";

            File file = new File(nomFichier);

            FileWriter writer = new FileWriter(file);

            LocalDateTime dateTime = d.getDateDemande();

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // 🔥 HEADER
            writer.append("ID;DATE;TYPE;DEP;DEST;ARTICLE;QTE;CAMION\n");

            // 🔥 LIGNES
            for (LigneDemande l : d.getLignes()) {

                writer.append(String.valueOf(d.getId())).append(";")
                        .append(dateTime.format(formatter)).append(";")
                        .append(l.getType() == TypeMouvement.ENTREE ? "E" : "S").append(";")
                        .append(d.getSiteDepart().getCodeSite()).append(";")
                        .append(d.getSiteArrivee().getCodeSite()).append(";")
                        .append(l.getArticle().getCodeArticle()).append(";")
                        .append(String.valueOf(l.getQuantite())).append(";")
                        .append(d.getCamion().getImmatriculation())
                        .append("\n");
            }

            writer.flush();
            writer.close();

            return file;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException("Erreur génération CSV");
        }
    }

    // 🔥 DEMANDEUR → ses demandes
    public List<Demande> getMesDemandes() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return demandeRepository.findByDemandeur(user);
    }

    // 🔥 VALIDATEUR → en attente
    public List<Demande> getByStatut(StatutDemande statut) {
        return demandeRepository.findByStatut(statut);
    }

    // 🔥 ADMIN
    public List<Demande> getAll() {
        return demandeRepository.findAll();
    }


    private String verifierDemande(Demande d) {

        // 1. demande vide
        if (d.getLignes() == null || d.getLignes().isEmpty()) {
            return "La demande doit contenir au moins un article";
        }

        // 2. quantité invalide
        boolean invalid = d.getLignes().stream()
                .anyMatch(l -> l.getQuantite() <= 0);

        if (invalid) {
            return "Quantité invalide";
        }

        // 3. capacité camion
        double total = d.getLignes().stream()
                .mapToDouble(l -> l.getQuantite())
                .sum();

        if (total > d.getCamion().getCapaciteReelle()) {
            return "Capacité du camion insuffisante";
        }

        // 4. sites identiques
        if (d.getSiteDepart().getId().equals(d.getSiteArrivee().getId())) {
            return "Site départ et arrivée identiques";
        }
        for (LigneDemande l : d.getLignes()) {

            if (l.getType() == TypeMouvement.SORTIE) {

                Article article = l.getArticle();

                if (article.getQuantite() < l.getQuantite()) {
                    return "Stock insuffisant pour l'article : " + article.getCodeArticle();
                }
            }
        }
        return null;
    }



}