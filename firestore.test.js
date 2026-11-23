const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-2944595a-9bac-47e3-98f7-2cdf5dcec5df";
const CLIENT_ALICE_UID = "client_alice";
const CLIENT_CHARLIE_UID = "client_charlie";
const PROVIDER_BOB_UID = "provider_bob";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: emulatorHost,
      port: emulatorPort,
      rules,
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

function getDb(auth) {
  // Using authenticated context with the named database
  return auth
    ? testEnv.authenticatedContext(auth.uid, auth.token || {}).firestore(DATABASE_ID)
    : testEnv.unauthenticatedContext().firestore(DATABASE_ID);
}

test("Unauthenticated access is denied on all collections", async () => {
  const unauthDb = getDb(null);
  await assertFails(unauthDb.collection("users").doc("test").get());
  await assertFails(unauthDb.collection("admins").doc("test").get());
  await assertFails(unauthDb.collection("services").doc("test").get());
  await assertFails(unauthDb.collection("proposals").doc("test").get());
  await assertFails(unauthDb.collection("services").doc("test").collection("messages").doc("msg").get());
  await assertFails(unauthDb.collection("users").doc("test").collection("documentReviews").doc("rev_1").get());
});

test("User profile: Client Alice can create and read own profile, Charlie cannot modify it", async () => {
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const charlieDb = getDb({ uid: CLIENT_CHARLIE_UID });

  // Alice creates her profile
  await assertSucceeds(
    aliceDb.collection("users").doc(CLIENT_ALICE_UID).set({
      userId: CLIENT_ALICE_UID,
      name: "Alice Silva",
      phone: "+5511999990001",
      role: "CLIENTE",
      createdAt: new Date(),
    })
  );

  // Alice can read her profile
  await assertSucceeds(aliceDb.collection("users").doc(CLIENT_ALICE_UID).get());

  // Charlie cannot modify Alice's profile
  await assertFails(
    charlieDb.collection("users").doc(CLIENT_ALICE_UID).update({
      name: "Hacked by Charlie",
    })
  );
});

test("Provider documents & privilege escalation protection: provider cannot self-approve or become ADMIN", async () => {
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });

  // Bob CANNOT create profile with role ADMIN
  await assertFails(
    bobDb.collection("users").doc(PROVIDER_BOB_UID).set({
      userId: PROVIDER_BOB_UID,
      name: "Bob Hacker",
      phone: "+5511999990002",
      role: "ADMIN",
      createdAt: new Date(),
    })
  );

  // Bob CANNOT create PRESTADOR profile already APROVADO
  await assertFails(
    bobDb.collection("users").doc(PROVIDER_BOB_UID).set({
      userId: PROVIDER_BOB_UID,
      name: "Bob Eletricista",
      phone: "+5511999990002",
      role: "PRESTADOR",
      verificationStatus: "APROVADO",
      createdAt: new Date(),
    })
  );

  // Bob CAN create PRESTADOR profile with PENDENTE_ANALISE and documents including attachment URIs
  await assertSucceeds(
    bobDb.collection("users").doc(PROVIDER_BOB_UID).set({
      userId: PROVIDER_BOB_UID,
      name: "Bob Eletricista",
      phone: "+5511999990002",
      role: "PRESTADOR",
      category: "Eletricista",
      verificationStatus: "PENDENTE_ANALISE",
      documents: {
        rgOrCnhNumber: "CNH 04829103948",
        rgOrCnhFileName: "cnh_bob.pdf",
        rgOrCnhAttachmentUri: "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
        criminalRecordCertNumber: "CERT-2026-99120-PF",
        criminalRecordFileName: "antecedentes_bob.pdf",
        criminalRecordAttachmentUri: "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
        selfieFileName: "selfie_bob.jpg",
        selfieAttachmentUri: "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
        submittedAt: Date.now(),
        idDocVerified: false,
        criminalRecordVerified: false,
        selfieVerified: false,
      },
      createdAt: new Date(),
    })
  );

  // Oversized attachment URI (> 250000 chars) MUST be rejected by rules
  await assertFails(
    bobDb.collection("users").doc("bob_oversized").set({
      userId: "bob_oversized",
      name: "Bob Oversized",
      phone: "+5511999990099",
      role: "PRESTADOR",
      verificationStatus: "PENDENTE_ANALISE",
      documents: {
        rgOrCnhNumber: "CNH 04829103948",
        rgOrCnhAttachmentUri: "a".repeat(260000),
      },
      createdAt: new Date(),
    })
  );

  // Bob CANNOT self-approve his verificationStatus
  await assertFails(
    bobDb.collection("users").doc(PROVIDER_BOB_UID).update({
      verificationStatus: "APROVADO",
    })
  );

  // Bob CANNOT change his role to ADMIN
  await assertFails(
    bobDb.collection("users").doc(PROVIDER_BOB_UID).update({
      role: "ADMIN",
    })
  );

  // Bob CANNOT write a documentReview audit entry himself
  await assertFails(
    bobDb.collection("users").doc(PROVIDER_BOB_UID).collection("documentReviews").doc("rev_fake").set({
      providerId: PROVIDER_BOB_UID,
      providerName: "Bob Eletricista",
      decision: "APROVADO",
      adminUid: PROVIDER_BOB_UID,
      createdAt: new Date(),
    })
  );
});

test("Admin Document Review: verified Admin can list providers, approve/reject, and write immutable documentReviews; unverified email spoofing fails", async () => {
  const ADMIN_UID = "admin_uid_1";
  const SPOOF_UID = "spoof_uid_2";

  // Seed provider and /admins/{adminUid} bypassing rules
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminCtxDb = context.firestore(DATABASE_ID);
    await adminCtxDb.collection("admins").doc(ADMIN_UID).set({
      role: "ADMIN",
      email: "admin@resolveai.com",
    });
    await adminCtxDb.collection("users").doc(PROVIDER_BOB_UID).set({
      userId: PROVIDER_BOB_UID,
      name: "Bob Eletricista",
      phone: "+5511999990002",
      role: "PRESTADOR",
      category: "Eletricista",
      verificationStatus: "PENDENTE_ANALISE",
      documents: {
        rgOrCnhNumber: "CNH 04829103948",
        rgOrCnhFileName: "cnh_bob.pdf",
        criminalRecordCertNumber: "CERT-2026-99120-PF",
        criminalRecordFileName: "antecedentes_bob.pdf",
        selfieFileName: "selfie_bob.jpg",
        idDocVerified: false,
        criminalRecordVerified: false,
        selfieVerified: false,
      },
      createdAt: new Date(),
    });
  });

  const adminDb = getDb({ uid: ADMIN_UID });
  const verifiedEmailAdminDb = getDb({
    uid: "google_admin_uid",
    token: { email: "edilsontcharlys2010@gmail.com", email_verified: true },
  });
  const unverifiedSpoofDb = getDb({
    uid: SPOOF_UID,
    token: { email: "edilsontcharlys2010@gmail.com", email_verified: false },
  });
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });

  // Unverified spoofed email CANNOT approve Bob
  await assertFails(
    unverifiedSpoofDb.collection("users").doc(PROVIDER_BOB_UID).update({
      verificationStatus: "APROVADO",
    })
  );

  // Regular client Alice CANNOT approve or reject Bob
  await assertFails(
    aliceDb.collection("users").doc(PROVIDER_BOB_UID).update({
      verificationStatus: "REJEITADO",
    })
  );

  // Regular client Alice CANNOT list all users (non-PRESTADOR / non-admin)
  await assertFails(aliceDb.collection("users").get());

  // Admin CAN list providers
  await assertSucceeds(adminDb.collection("users").where("role", "==", "PRESTADOR").get());

  // Verified email Admin CAN reject Bob with reason + write audit review
  await assertSucceeds(
    verifiedEmailAdminDb.collection("users").doc(PROVIDER_BOB_UID).update({
      verificationStatus: "REJEITADO",
      documents: {
        rgOrCnhNumber: "CNH 04829103948",
        rgOrCnhFileName: "cnh_bob.pdf",
        criminalRecordCertNumber: "CERT-2026-99120-PF",
        criminalRecordFileName: "antecedentes_bob.pdf",
        selfieFileName: "selfie_bob.jpg",
        rejectionReason: "Foto da CNH com reflexo ilegível",
        reviewedBy: "google_admin_uid",
        reviewedByEmail: "edilsontcharlys2010@gmail.com",
        idDocVerified: false,
        criminalRecordVerified: true,
        selfieVerified: true,
      },
    })
  );

  await assertSucceeds(
    verifiedEmailAdminDb
      .collection("users")
      .doc(PROVIDER_BOB_UID)
      .collection("documentReviews")
      .doc("rev_reject_1")
      .set({
        providerId: PROVIDER_BOB_UID,
        providerName: "Bob Eletricista",
        decision: "REJEITADO",
        reason: "Foto da CNH com reflexo ilegível",
        adminUid: "google_admin_uid",
        adminEmail: "edilsontcharlys2010@gmail.com",
        idDocVerified: false,
        criminalRecordVerified: true,
        selfieVerified: true,
        createdAt: new Date(),
      })
  );

  // Admin via /admins/{uid} CAN approve Bob + write audit review
  await assertSucceeds(
    adminDb.collection("users").doc(PROVIDER_BOB_UID).update({
      verificationStatus: "APROVADO",
      documents: {
        rgOrCnhNumber: "CNH 04829103948",
        rgOrCnhFileName: "cnh_bob.pdf",
        criminalRecordCertNumber: "CERT-2026-99120-PF",
        criminalRecordFileName: "antecedentes_bob.pdf",
        selfieFileName: "selfie_bob.jpg",
        reviewedBy: ADMIN_UID,
        idDocVerified: true,
        criminalRecordVerified: true,
        selfieVerified: true,
      },
    })
  );

  await assertSucceeds(
    adminDb
      .collection("users")
      .doc(PROVIDER_BOB_UID)
      .collection("documentReviews")
      .doc("rev_approve_1")
      .set({
        providerId: PROVIDER_BOB_UID,
        providerName: "Bob Eletricista",
        decision: "APROVADO",
        adminUid: ADMIN_UID,
        idDocVerified: true,
        criminalRecordVerified: true,
        selfieVerified: true,
        createdAt: new Date(),
      })
  );

  // Owner Bob and Admin can read Bob's review history; Alice cannot
  await assertSucceeds(bobDb.collection("users").doc(PROVIDER_BOB_UID).collection("documentReviews").get());
  await assertSucceeds(adminDb.collection("users").doc(PROVIDER_BOB_UID).collection("documentReviews").get());
  await assertFails(aliceDb.collection("users").doc(PROVIDER_BOB_UID).collection("documentReviews").get());

  // documentReviews is strictly immutable (update/delete denied even for admin)
  await assertFails(
    adminDb.collection("users").doc(PROVIDER_BOB_UID).collection("documentReviews").doc("rev_approve_1").update({
      decision: "REJEITADO",
    })
  );
  await assertFails(
    adminDb.collection("users").doc(PROVIDER_BOB_UID).collection("documentReviews").doc("rev_approve_1").delete()
  );
});

test("Service Requests: Client Alice creates service; Client Charlie cannot delete or tamper with it", async () => {
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const charlieDb = getDb({ uid: CLIENT_CHARLIE_UID });

  // Alice creates service
  await assertSucceeds(
    aliceDb.collection("services").doc("srv_1").set({
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      title: "Reparo de fiação chuveiro",
      category: "Eletricista",
      proposedBudget: 280.0,
      status: "ABERTO",
      createdAt: new Date(),
    })
  );

  // Charlie cannot update Alice's budget
  await assertFails(
    charlieDb.collection("services").doc("srv_1").update({
      proposedBudget: 10.0,
    })
  );

  // Charlie cannot delete Alice's service
  await assertFails(charlieDb.collection("services").doc("srv_1").delete());
});

test("Proposals: Provider Bob submits proposal; Alice & Bob can read; Charlie cannot read or tamper", async () => {
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });
  const charlieDb = getDb({ uid: CLIENT_CHARLIE_UID });

  // Bob creates proposal for Alice's service
  await assertSucceeds(
    bobDb.collection("proposals").doc("prop_1").set({
      serviceId: "srv_1",
      clientId: CLIENT_ALICE_UID,
      providerId: PROVIDER_BOB_UID,
      providerName: "Bob Eletricista",
      proposedPrice: 280.0,
      status: "ENVIADA",
      createdAt: new Date(),
    })
  );

  // Bob and Alice can read the proposal
  await assertSucceeds(bobDb.collection("proposals").doc("prop_1").get());
  await assertSucceeds(aliceDb.collection("proposals").doc("prop_1").get());

  // Charlie (another client/provider) CANNOT read the proposal
  await assertFails(charlieDb.collection("proposals").doc("prop_1").get());

  // Charlie cannot update proposal
  await assertFails(
    charlieDb.collection("proposals").doc("prop_1").update({
      status: "RECUSADA",
    })
  );
});

test("Private Chat: Chat is forbidden when service is not accepted; allowed once accepted for participants", async () => {
  // Setup service in admin mode (using withSecurityRulesDisabled)
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore(DATABASE_ID);
    await adminDb.collection("services").doc("srv_negotiating").set({
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      title: "Serviço em negociação",
      category: "Eletricista",
      proposedBudget: 250.0,
      status: "EM_NEGOCIACAO",
      createdAt: new Date(),
    });

    await adminDb.collection("services").doc("srv_accepted").set({
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      title: "Serviço aceito",
      category: "Eletricista",
      proposedBudget: 280.0,
      status: "ACEITO",
      assignedProviderId: PROVIDER_BOB_UID,
      assignedProviderName: "Bob Eletricista",
      createdAt: new Date(),
    });
  });

  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });
  const charlieDb = getDb({ uid: CLIENT_CHARLIE_UID });

  // In unaccepted service, chat message is rejected
  await assertFails(
    aliceDb.collection("services").doc("srv_negotiating").collection("messages").doc("msg_1").set({
      serviceId: "srv_negotiating",
      senderId: CLIENT_ALICE_UID,
      senderName: "Alice",
      senderRole: "CLIENTE",
      content: "Olá Bob",
      createdAt: new Date(),
    })
  );

  // In accepted service, Alice and Bob can chat
  await assertSucceeds(
    aliceDb.collection("services").doc("srv_accepted").collection("messages").doc("msg_2").set({
      serviceId: "srv_accepted",
      senderId: CLIENT_ALICE_UID,
      senderName: "Alice",
      senderRole: "CLIENTE",
      content: "Olá Bob, combinado o horário para amanhã!",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    bobDb.collection("services").doc("srv_accepted").collection("messages").doc("msg_3").set({
      serviceId: "srv_accepted",
      senderId: PROVIDER_BOB_UID,
      senderName: "Bob",
      senderRole: "PRESTADOR",
      content: "Perfeito Alice, estarei aí pontualmente.",
      createdAt: new Date(),
    })
  );

  // Charlie CANNOT read or write in Alice and Bob's chat
  await assertFails(
    charlieDb.collection("services").doc("srv_accepted").collection("messages").doc("msg_2").get()
  );
  await assertFails(
    charlieDb.collection("services").doc("srv_accepted").collection("messages").doc("msg_4").set({
      serviceId: "srv_accepted",
      senderId: CLIENT_CHARLIE_UID,
      senderName: "Charlie",
      senderRole: "CLIENTE",
      content: "Invasão no chat",
      createdAt: new Date(),
    })
  );
});

test("Service Execution Lifecycle & PhotoUrl: EM_EXECUCAO status, photoUrl updates, and acceptedProvider fields succeed for participants", async () => {
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });

  await assertSucceeds(
    aliceDb.collection("users").doc(CLIENT_ALICE_UID).set({
      userId: CLIENT_ALICE_UID,
      name: "Alice Silva",
      phone: "+5511999990001",
      role: "CLIENTE",
      photoUrl: "https://example.com/alice.jpg",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(CLIENT_ALICE_UID).update({
      photoUrl: "https://example.com/alice_v2.jpg",
    })
  );

  await assertSucceeds(
    aliceDb.collection("services").doc("srv_exec_1").set({
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      clientRating: 4.9,
      clientAvatarColorHex: 0x2563eb,
      clientPhotoUrl: "https://example.com/alice_v2.jpg",
      title: "Instalação elétrica residencial",
      category: "Eletricista",
      proposedBudget: 320.0,
      status: "ABERTO",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("services").doc("srv_exec_1").update({
      status: "EM_EXECUCAO",
      assignedProviderId: PROVIDER_BOB_UID,
      assignedProviderName: "Bob Eletricista",
      acceptedProviderId: PROVIDER_BOB_UID,
      acceptedProviderName: "Bob Eletricista",
      acceptedProviderRating: 4.95,
      acceptedProviderAvatarColorHex: 0x0d9488,
      acceptedProviderPhotoUrl: "https://example.com/bob.jpg",
      finalAgreedPrice: 320.0,
      platformFee: 32.0,
    })
  );

  await assertSucceeds(
    bobDb.collection("services").doc("srv_exec_1").collection("messages").doc("msg_exec").set({
      serviceId: "srv_exec_1",
      senderId: PROVIDER_BOB_UID,
      senderName: "Bob Eletricista",
      senderRole: "PRESTADOR",
      content: "Estou a caminho para iniciar a execução.",
      createdAt: new Date(),
    })
  );

  // System notices written on acceptance/completion use senderRole: "SISTEMA"
  await assertSucceeds(
    aliceDb.collection("services").doc("srv_exec_1").collection("messages").doc("msg_sys").set({
      serviceId: "srv_exec_1",
      senderId: CLIENT_ALICE_UID,
      senderName: "Sistema ResolveAi",
      senderRole: "SISTEMA",
      content: "Serviço confirmado entre as partes.",
      createdAt: new Date(),
    })
  );

  // Service using acceptedProviderId only (without assignedProviderId) still allows participant chat
  await assertSucceeds(
    aliceDb.collection("services").doc("srv_accepted_only").set({
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      title: "Pintura de sala",
      category: "Pintor",
      proposedBudget: 400.0,
      status: "ACEITO",
      acceptedProviderId: PROVIDER_BOB_UID,
      acceptedProviderName: "Bob Eletricista",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    bobDb.collection("services").doc("srv_accepted_only").collection("messages").doc("msg_bob").set({
      serviceId: "srv_accepted_only",
      senderId: PROVIDER_BOB_UID,
      senderName: "Bob Eletricista",
      senderRole: "PRESTADOR",
      content: "Mensagem com acceptedProviderId funcionando.",
      createdAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("services").doc("srv_accepted_only").collection("messages").doc("msg_bob").get()
  );
});

test("LocationHistory & AppSuggestions & ClientCompletionPhotos follow strict access control", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore(DATABASE_ID);
    await adminDb.collection("services").doc("srv_loc_1").set({
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      title: "Instalação hidráulica",
      category: "Encanador",
      proposedBudget: 250.0,
      status: "ACEITO",
      assignedProviderId: PROVIDER_BOB_UID,
      acceptedProviderId: PROVIDER_BOB_UID,
      createdAt: new Date(),
    });
  });

  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });
  const charlieDb = getDb({ uid: CLIENT_CHARLIE_UID });
  const adminDb = getDb({
    uid: "admin_uid",
    token: { email: "edilsontcharlys2010@gmail.com", email_verified: true },
  });

  // Assigned provider Bob can append immutable locationHistory points
  await assertSucceeds(
    bobDb.collection("services").doc("srv_loc_1").collection("locationHistory").doc("loc_1").set({
      serviceId: "srv_loc_1",
      providerId: PROVIDER_BOB_UID,
      latitude: -23.55052,
      longitude: -46.633308,
      accuracyMeters: 8.5,
      serviceStatus: "ACEITO",
      source: "GPS",
      createdAt: new Date(),
    })
  );

  // Unassigned user Charlie cannot read or write Bob's locationHistory
  await assertFails(
    charlieDb.collection("services").doc("srv_loc_1").collection("locationHistory").doc("loc_1").get()
  );
  await assertFails(
    charlieDb.collection("services").doc("srv_loc_1").collection("locationHistory").doc("loc_2").set({
      serviceId: "srv_loc_1",
      providerId: CLIENT_CHARLIE_UID,
      latitude: -23.55,
      longitude: -46.63,
      createdAt: new Date(),
    })
  );

  // Client Alice can attach clientCompletionPhotos on concluded service; Charlie cannot
  await assertSucceeds(
    aliceDb.collection("services").doc("srv_loc_1").update({
      status: "CONCLUIDO",
      clientCompletionPhotos: [{ id: "cp_1", photoUri: "data:image/jpeg;base64,abc", photoName: "ok.jpg" }],
    })
  );
  await assertFails(
    charlieDb.collection("services").doc("srv_loc_1").update({
      clientCompletionPhotos: [{ id: "cp_x", photoUri: "bad", photoName: "bad.jpg" }],
    })
  );

  // AppSuggestions: Alice can create her own suggestion with status ENVIADA; Admin can update status
  await assertSucceeds(
    aliceDb.collection("appSuggestions").doc("sug_1").set({
      userId: CLIENT_ALICE_UID,
      userName: "Alice Silva",
      userRole: "CLIENTE",
      category: "Melhoria no App",
      title: "Modo escuro automático",
      description: "Seguir o tema do sistema operacional.",
      status: "ENVIADA",
      createdAt: new Date(),
    })
  );

  await assertFails(
    charlieDb.collection("appSuggestions").doc("sug_1").get()
  );
  await assertFails(
    aliceDb.collection("appSuggestions").doc("sug_1").update({ status: "IMPLEMENTADA" })
  );
  await assertSucceeds(
    adminDb.collection("appSuggestions").doc("sug_1").update({
      status: "IMPLEMENTADA",
      adminReply: "Obrigado pela sugestão!",
    })
  );
});

test("ServiceCategories & AppConfig: signed-in users can read, only verified Admin can write valid docs", async () => {
  const unauthDb = getDb(null);
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const adminDb = getDb({
    uid: "admin_uid_cfg",
    token: { email: "edilsontcharlys2010@gmail.com", email_verified: true },
  });

  // Unauthenticated access denied
  await assertFails(unauthDb.collection("serviceCategories").doc("cat_jardinagem").get());
  await assertFails(unauthDb.collection("appConfig").doc("general").get());

  // Regular client Alice CANNOT write categories or appConfig
  await assertFails(
    aliceDb.collection("serviceCategories").doc("cat_jardinagem").set({
      id: "cat_jardinagem",
      name: "Jardinagem",
      specialtyLabel: "Jardinagem & Paisagismo",
      isActive: true,
      orderIndex: 11,
      updatedAt: new Date(),
    })
  );
  await assertFails(
    aliceDb.collection("appConfig").doc("general").set({
      appDisplayName: "ResolveAi Hacked",
      supportWhatsappNumberE164: "5561992907464",
    })
  );

  // Admin CAN create, update, and delete valid category docs; invalid name/id fails
  await assertFails(
    adminDb.collection("serviceCategories").doc("cat_bad").set({
      id: "wrong_id",
      name: "A",
      isActive: true,
      orderIndex: 0,
    })
  );
  await assertSucceeds(
    adminDb.collection("serviceCategories").doc("cat_jardinagem").set({
      id: "cat_jardinagem",
      name: "Jardinagem",
      specialtyLabel: "Jardinagem & Paisagismo",
      isActive: true,
      orderIndex: 11,
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    adminDb.collection("serviceCategories").doc("cat_jardinagem").update({
      name: "Jardinagem Express",
      isActive: false,
    })
  );

  // Admin CAN write valid appConfig; invalid urgency fails
  await assertFails(
    adminDb.collection("appConfig").doc("general").set({
      appDisplayName: "ResolveAi Serviços",
      defaultServiceUrgency: "InvalidaUrgencia",
    })
  );
  await assertSucceeds(
    adminDb.collection("appConfig").doc("general").set({
      appDisplayName: "ResolveAi Serviços",
      globalBannerMessage: "Atendimento em todo o Brasil!",
      supportWhatsappNumberE164: "5561992907464",
      supportWhatsappDisplay: "(61) 99290-7464",
      defaultServiceUrgency: "Média",
      tips: [
        {
          id: "tip_1",
          category: "Dica",
          title: "Descreva bem seu pedido",
          description: "Informe detalhes e fotos para agilizar respostas.",
        },
      ],
      updatedAt: new Date(),
    })
  );

  // Regular signed-in user Alice CAN read categories and appConfig
  await assertSucceeds(aliceDb.collection("serviceCategories").doc("cat_jardinagem").get());
  await assertSucceeds(aliceDb.collection("appConfig").doc("general").get());

  // Admin CAN delete category and appConfig
  await assertSucceeds(adminDb.collection("serviceCategories").doc("cat_jardinagem").delete());
  await assertSucceeds(adminDb.collection("appConfig").doc("general").delete());
});

test("Negative Provider Wallet Balance & Direct Acceptance Proposals are permitted by security rules", async () => {
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });

  // Provider Bob created with R$ 15.00 balance
  await assertSucceeds(
    bobDb.collection("users").doc(PROVIDER_BOB_UID).set({
      userId: PROVIDER_BOB_UID,
      name: "Bob Eletricista",
      phone: "+5511999990002",
      role: "PRESTADOR",
      walletBalance: 15.0,
      createdAt: new Date(),
    })
  );

  // Updating walletBalance to negative value (e.g. -25.0 after 10% fee debit) and logging transaction succeeds
  await assertSucceeds(
    bobDb.collection("users").doc(PROVIDER_BOB_UID).update({
      walletBalance: -25.0,
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(PROVIDER_BOB_UID).collection("transactions").doc("tx_neg_1").set({
      providerId: PROVIDER_BOB_UID,
      type: "DEBITO_TAXA_10",
      amount: 40.0,
      balanceAfter: -25.0,
      serviceId: "srv_1",
      description: "Taxa de intermediação de 10%",
      createdAt: new Date(),
    })
  );

  // Direct acceptance proposal (isDirectAcceptance: true) can be created by Bob and accepted by Alice
  await assertSucceeds(
    bobDb.collection("proposals").doc("prop_direct_1").set({
      serviceId: "srv_1",
      clientId: CLIENT_ALICE_UID,
      providerId: PROVIDER_BOB_UID,
      providerName: "Bob Eletricista",
      proposedPrice: 400.0,
      status: "ENVIADA",
      isDirectAcceptance: true,
      createdAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("proposals").doc("prop_direct_1").update({
      status: "ACEITA",
      isDirectAcceptance: true,
    })
  );
});

test("Post-service 1..5 star reviews and provider replies enforce strict Firestore security rules", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore(DATABASE_ID);
    await adminDb.collection("services").doc("srv_open_rev").set({
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      title: "Serviço ainda aberto",
      category: "Eletricista",
      proposedBudget: 150.0,
      status: "ACEITO",
      assignedProviderId: PROVIDER_BOB_UID,
      acceptedProviderId: PROVIDER_BOB_UID,
      createdAt: new Date(),
    });
    await adminDb.collection("services").doc("srv_done_rev").set({
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      title: "Instalação elétrica finalizada",
      category: "Eletricista",
      proposedBudget: 280.0,
      status: "CONCLUIDO",
      assignedProviderId: PROVIDER_BOB_UID,
      acceptedProviderId: PROVIDER_BOB_UID,
      acceptedProviderName: "Bob Eletricista",
      createdAt: new Date(),
    });
  });

  const unauthDb = getDb(null);
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });
  const charlieDb = getDb({ uid: CLIENT_CHARLIE_UID });

  // Unauthenticated cannot read reviews
  await assertFails(unauthDb.collection("reviews").doc("srv_done_rev").get());

  // Cannot review a service that is not CONCLUIDO
  await assertFails(
    aliceDb.collection("reviews").doc("srv_open_rev").set({
      serviceId: "srv_open_rev",
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      providerId: PROVIDER_BOB_UID,
      providerName: "Bob Eletricista",
      rating: 5,
      comment: "Tentativa antes da conclusão",
      isPublic: true,
      createdAt: new Date(),
    })
  );

  // Out-of-range rating (0 or 6) is rejected
  await assertFails(
    aliceDb.collection("reviews").doc("srv_done_rev").set({
      serviceId: "srv_done_rev",
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      providerId: PROVIDER_BOB_UID,
      providerName: "Bob Eletricista",
      rating: 6,
      comment: "Nota fora do limite",
      isPublic: true,
      createdAt: new Date(),
    })
  );

  // Charlie cannot review Alice's service; Provider Bob cannot self-review
  await assertFails(
    charlieDb.collection("reviews").doc("srv_done_rev").set({
      serviceId: "srv_done_rev",
      clientId: CLIENT_CHARLIE_UID,
      clientName: "Charlie",
      providerId: PROVIDER_BOB_UID,
      providerName: "Bob Eletricista",
      rating: 1,
      comment: "Fake",
      isPublic: true,
      createdAt: new Date(),
    })
  );
  await assertFails(
    bobDb.collection("reviews").doc("srv_done_rev").set({
      serviceId: "srv_done_rev",
      clientId: PROVIDER_BOB_UID,
      clientName: "Bob",
      providerId: PROVIDER_BOB_UID,
      providerName: "Bob Eletricista",
      rating: 5,
      comment: "Autoavaliação",
      isPublic: true,
      createdAt: new Date(),
    })
  );

  // Alice CAN create 5-star review on her CONCLUIDO service and update service rating fields
  await assertSucceeds(
    aliceDb.collection("reviews").doc("srv_done_rev").set({
      serviceId: "srv_done_rev",
      serviceTitle: "Instalação elétrica finalizada",
      serviceCategory: "Eletricista",
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      providerId: PROVIDER_BOB_UID,
      providerName: "Bob Eletricista",
      rating: 5,
      comment: "Pontual e muito caprichoso!",
      complimentTags: ["Pontual", "Caprichoso"],
      isPublic: true,
      createdAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("services").doc("srv_done_rev").update({
      ratingGiven: 5,
      reviewComment: "Pontual e muito caprichoso!",
      reviewTags: ["Pontual", "Caprichoso"],
      reviewedAt: Date.now(),
    })
  );

  // Alice CANNOT overwrite ratingGiven a second time
  await assertFails(
    aliceDb.collection("services").doc("srv_done_rev").update({
      ratingGiven: 2,
    })
  );

  // Signed-in users CAN read single public review and list with isPublic == true (unfiltered list denied)
  await assertSucceeds(charlieDb.collection("reviews").doc("srv_done_rev").get());
  await assertSucceeds(charlieDb.collection("reviews").where("isPublic", "==", true).get());
  await assertFails(charlieDb.collection("reviews").get());

  // Charlie and Alice CANNOT write a providerReply
  await assertFails(
    aliceDb.collection("reviews").doc("srv_done_rev").update({
      providerReply: "Resposta falsa",
      repliedAt: new Date(),
    })
  );

  // Provider Bob CANNOT tamper with rating, but CAN reply once
  await assertFails(
    bobDb.collection("reviews").doc("srv_done_rev").update({
      rating: 1,
      providerReply: "Obrigado",
      repliedAt: new Date(),
    })
  );
  await assertFails(
    bobDb.collection("reviews").doc("srv_done_rev").update({
      comment: "Alterado pelo prestador",
      providerReply: "Obrigado",
      repliedAt: new Date(),
    })
  );
  await assertSucceeds(
    bobDb.collection("reviews").doc("srv_done_rev").update({
      providerReply: "Muito obrigado pela confiança, Alice!",
      repliedAt: new Date(),
    })
  );

  // Provider Bob CANNOT reply twice
  await assertFails(
    bobDb.collection("reviews").doc("srv_done_rev").update({
      providerReply: "Tentativa de editar resposta",
      repliedAt: new Date(),
    })
  );

  // Client Alice can mark an EM_EXECUCAO service as CONCLUIDO (with completedAt) and then rate Bob 5 stars
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminCtxDb = context.firestore(DATABASE_ID);
    await adminCtxDb.collection("services").doc("srv_client_finish").set({
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      title: "Revisão elétrica completa",
      category: "Eletricista",
      proposedBudget: 350.0,
      status: "EM_EXECUCAO",
      assignedProviderId: PROVIDER_BOB_UID,
      acceptedProviderId: PROVIDER_BOB_UID,
      acceptedProviderName: "Bob Eletricista",
      createdAt: new Date(),
    });
  });

  await assertSucceeds(
    aliceDb.collection("services").doc("srv_client_finish").update({
      status: "CONCLUIDO",
      locationTrackingActive: false,
      completedAt: Date.now(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("reviews").doc("srv_client_finish").set({
      serviceId: "srv_client_finish",
      serviceTitle: "Revisão elétrica completa",
      serviceCategory: "Eletricista",
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      providerId: PROVIDER_BOB_UID,
      providerName: "Bob Eletricista",
      rating: 4,
      comment: "Ótimo trabalho!",
      complimentTags: ["Pontual"],
      isPublic: true,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("services").doc("srv_client_finish").update({
      ratingGiven: 4,
      reviewComment: "Ótimo trabalho!",
      reviewTags: ["Pontual"],
      acceptedProviderRating: 4.5,
      reviewedAt: new Date(),
    })
  );
});

test("Multi-provider selection: rejecting competing provider and restricting chat strictly to accepted provider", async () => {
  const PROVIDER_DAVE_UID = "provider_dave";
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });
  const daveDb = getDb({ uid: PROVIDER_DAVE_UID });

  await assertSucceeds(
    aliceDb.collection("services").doc("srv_choice").set({
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Silva",
      title: "Troca de torneira gourmet",
      category: "Hidráulica",
      proposedBudget: 200.0,
      status: "EM_NEGOCIACAO",
      createdAt: new Date(),
    })
  );

  // Both Bob and Dave send direct acceptance proposals
  await assertSucceeds(
    bobDb.collection("proposals").doc("prop_bob").set({
      serviceId: "srv_choice",
      clientId: CLIENT_ALICE_UID,
      providerId: PROVIDER_BOB_UID,
      providerName: "Bob",
      proposedPrice: 200.0,
      status: "ENVIADA",
      isDirectAcceptance: true,
      createdAt: new Date(),
    })
  );
  await assertSucceeds(
    daveDb.collection("proposals").doc("prop_dave").set({
      serviceId: "srv_choice",
      clientId: CLIENT_ALICE_UID,
      providerId: PROVIDER_DAVE_UID,
      providerName: "Dave",
      proposedPrice: 200.0,
      status: "ENVIADA",
      isDirectAcceptance: true,
      createdAt: new Date(),
    })
  );

  // Before client accepts anyone, chat is blocked for both Bob and Dave
  await assertFails(
    bobDb.collection("services").doc("srv_choice").collection("messages").doc("m_early").set({
      serviceId: "srv_choice",
      senderId: PROVIDER_BOB_UID,
      senderName: "Bob",
      senderRole: "PRESTADOR",
      content: "Oi antes do aceite",
      createdAt: new Date(),
    })
  );

  // Alice rejects Dave and accepts Bob
  await assertSucceeds(
    aliceDb.collection("proposals").doc("prop_dave").update({ status: "RECUSADA" })
  );
  await assertSucceeds(
    aliceDb.collection("proposals").doc("prop_bob").update({ status: "ACEITA" })
  );
  await assertSucceeds(
    aliceDb.collection("services").doc("srv_choice").update({
      status: "ACEITO",
      assignedProviderId: PROVIDER_BOB_UID,
      assignedProviderName: "Bob",
      acceptedProviderId: PROVIDER_BOB_UID,
      acceptedProviderName: "Bob",
    })
  );

  // Only Alice and accepted provider Bob can write/read chat; Dave is blocked
  await assertSucceeds(
    bobDb.collection("services").doc("srv_choice").collection("messages").doc("m_bob").set({
      serviceId: "srv_choice",
      senderId: PROVIDER_BOB_UID,
      senderName: "Bob",
      senderRole: "PRESTADOR",
      content: "Combinado, Alice!",
      createdAt: new Date(),
    })
  );
  await assertFails(
    daveDb.collection("services").doc("srv_choice").collection("messages").doc("m_bob").get()
  );
  await assertFails(
    daveDb.collection("services").doc("srv_choice").collection("messages").doc("m_dave").set({
      serviceId: "srv_choice",
      senderId: PROVIDER_DAVE_UID,
      senderName: "Dave",
      senderRole: "PRESTADOR",
      content: "Quero entrar no chat",
      createdAt: new Date(),
    })
  );
});

test("Admin Manual Balance Credit: only Admin can create CREDITO_ADMIN transaction and add balance to provider", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const setupDb = context.firestore(DATABASE_ID);
    await setupDb.collection("users").doc(PROVIDER_BOB_UID).set({
      userId: PROVIDER_BOB_UID,
      name: "Bob Eletricista",
      phone: "+5511999990002",
      role: "PRESTADOR",
      walletBalance: 25.0,
      createdAt: new Date(),
    });
  });

  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });
  const adminDb = getDb({
    uid: "admin_wallet_uid",
    token: { email: "edilsontcharlys2010@gmail.com", email_verified: true },
  });

  // Client Alice cannot alter Bob's walletBalance or write CREDITO_ADMIN
  await assertFails(
    aliceDb.collection("users").doc(PROVIDER_BOB_UID).update({ walletBalance: 500.0 })
  );
  await assertFails(
    aliceDb.collection("users").doc(PROVIDER_BOB_UID).collection("transactions").doc("tx_fake_1").set({
      providerId: PROVIDER_BOB_UID,
      type: "CREDITO_ADMIN",
      amount: 100.0,
      balanceAfter: 125.0,
      description: "Tentativa não autorizada",
      createdAt: new Date(),
    })
  );

  // Provider Bob himself cannot forge a CREDITO_ADMIN transaction
  await assertFails(
    bobDb.collection("users").doc(PROVIDER_BOB_UID).collection("transactions").doc("tx_fake_bob").set({
      providerId: PROVIDER_BOB_UID,
      type: "CREDITO_ADMIN",
      amount: 100.0,
      balanceAfter: 125.0,
      description: "Auto-crédito admin indevido",
      createdAt: new Date(),
    })
  );

  // Verified Admin CAN credit Bob's walletBalance and record immutable CREDITO_ADMIN transaction
  await assertSucceeds(
    adminDb.collection("users").doc(PROVIDER_BOB_UID).update({
      walletBalance: 125.0,
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(PROVIDER_BOB_UID).collection("transactions").doc("tx_admin_1").set({
      providerId: PROVIDER_BOB_UID,
      type: "CREDITO_ADMIN",
      amount: 100.0,
      balanceAfter: 125.0,
      description: "Crédito manual pelo administrador: Bônus de parceiro",
      createdAt: new Date(),
    })
  );
});

test("Provider can record declinedServiceIds on their own user profile while others cannot", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const setupDb = context.firestore(DATABASE_ID);
    await setupDb.collection("users").doc(PROVIDER_BOB_UID).set({
      userId: PROVIDER_BOB_UID,
      name: "Bob Eletricista",
      phone: "+5511999990002",
      role: "PRESTADOR",
      walletBalance: 25.0,
      createdAt: new Date(),
    });
  });

  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });

  await assertSucceeds(
    bobDb.collection("users").doc(PROVIDER_BOB_UID).update({
      declinedServiceIds: ["srv_101", "srv_202"],
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(PROVIDER_BOB_UID).update({
      declinedServiceIds: ["srv_999"],
    })
  );
});

test("User Notifications: participant can create for recipient; owner can read & mark isRead only; cross-user read denied; admin can read/delete", async () => {
  const unauthDb = getDb(null);
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });
  const charlieDb = getDb({ uid: CLIENT_CHARLIE_UID });
  const adminDb = getDb({
    uid: "admin_notif_uid",
    token: { email: "edilsontcharlys2010@gmail.com", email_verified: true },
  });

  // Unauthenticated cannot create or read notifications
  await assertFails(
    unauthDb.collection("users").doc(CLIENT_ALICE_UID).collection("notifications").doc("notif_unauth").set({
      recipientId: CLIENT_ALICE_UID,
      title: "Spam",
      message: "Sem login",
      type: "NOVA_MENSAGEM",
      isRead: false,
      createdAt: new Date(),
    })
  );

  // Provider Bob creates a notification for Client Alice
  await assertSucceeds(
    bobDb.collection("users").doc(CLIENT_ALICE_UID).collection("notifications").doc("notif_1").set({
      recipientId: CLIENT_ALICE_UID,
      title: "Nova Proposta Recebida",
      message: "Bob enviou uma proposta de R$ 280,00.",
      type: "PROPOSTA_RECEBIDA",
      serviceId: "srv_1",
      isRead: false,
      senderId: PROVIDER_BOB_UID,
      createdAt: new Date(),
    })
  );

  // Alice (owner) can get and list her own notifications
  await assertSucceeds(aliceDb.collection("users").doc(CLIENT_ALICE_UID).collection("notifications").doc("notif_1").get());
  await assertSucceeds(aliceDb.collection("users").doc(CLIENT_ALICE_UID).collection("notifications").get());

  // Charlie cannot read Alice's notifications
  await assertFails(charlieDb.collection("users").doc(CLIENT_ALICE_UID).collection("notifications").doc("notif_1").get());
  await assertFails(charlieDb.collection("users").doc(CLIENT_ALICE_UID).collection("notifications").get());

  // Alice can mark her notification as read (only isRead / updatedAt)
  await assertSucceeds(
    aliceDb.collection("users").doc(CLIENT_ALICE_UID).collection("notifications").doc("notif_1").update({
      isRead: true,
    })
  );

  // Alice cannot tamper with message or recipientId
  await assertFails(
    aliceDb.collection("users").doc(CLIENT_ALICE_UID).collection("notifications").doc("notif_1").update({
      message: "Mensagem adulterada",
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(CLIENT_ALICE_UID).collection("notifications").doc("notif_1").update({
      recipientId: CLIENT_CHARLIE_UID,
    })
  );

  // Admin can read and delete notifications
  await assertSucceeds(adminDb.collection("users").doc(CLIENT_ALICE_UID).collection("notifications").get());
  await assertSucceeds(adminDb.collection("users").doc(CLIENT_ALICE_UID).collection("notifications").doc("notif_1").delete());
});

test("Admin User Credentials: strictly admin-only read/list/delete; user can write only own doc; cross-user write denied", async () => {
  const unauthDb = getDb(null);
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });
  const adminDb = getDb({
    uid: "admin_cred_uid",
    token: { email: "edilsontcharlys2010@gmail.com", email_verified: true },
  });

  // Unauthenticated denied
  await assertFails(unauthDb.collection("adminUserCredentials").doc(CLIENT_ALICE_UID).get());
  await assertFails(unauthDb.collection("adminUserCredentials").get());

  // Alice can write her own credential record
  await assertSucceeds(
    aliceDb.collection("adminUserCredentials").doc(CLIENT_ALICE_UID).set({
      userId: CLIENT_ALICE_UID,
      displayPassword: "SenhaSegura123",
      updatedBy: CLIENT_ALICE_UID,
      updatedAt: Date.now(),
    })
  );

  // Alice CANNOT read/list even her own adminUserCredentials doc (strictly admin-only read)
  await assertFails(aliceDb.collection("adminUserCredentials").doc(CLIENT_ALICE_UID).get());
  await assertFails(aliceDb.collection("adminUserCredentials").get());

  // Bob CANNOT write to Alice's credential doc or read it
  await assertFails(
    bobDb.collection("adminUserCredentials").doc(CLIENT_ALICE_UID).set({
      userId: CLIENT_ALICE_UID,
      displayPassword: "HackedPassword",
      updatedBy: PROVIDER_BOB_UID,
      updatedAt: Date.now(),
    })
  );
  await assertFails(bobDb.collection("adminUserCredentials").doc(CLIENT_ALICE_UID).get());

  // Verified Admin CAN get, list, update, and delete any user's credential record
  await assertSucceeds(adminDb.collection("adminUserCredentials").doc(CLIENT_ALICE_UID).get());
  await assertSucceeds(adminDb.collection("adminUserCredentials").get());
  await assertSucceeds(
    adminDb.collection("adminUserCredentials").doc(CLIENT_ALICE_UID).update({
      displayPassword: "NovaSenhaAdmin99",
      updatedBy: "admin_cred_uid",
      updatedAt: Date.now(),
    })
  );
  await assertSucceeds(adminDb.collection("adminUserCredentials").doc(CLIENT_ALICE_UID).delete());
});

test("Pix Charges: owner and admin can list pixCharges; other users cannot list or read", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const setupDb = context.firestore(DATABASE_ID);
    await setupDb.collection("users").doc(PROVIDER_BOB_UID).collection("pixCharges").doc("pix_list_1").set({
      txId: "pix_list_1",
      providerId: PROVIDER_BOB_UID,
      amount: 50.0,
      status: "AGUARDANDO_PAGAMENTO",
      createdAt: new Date(),
    });
  });

  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });
  const adminDb = getDb({
    uid: "admin_pix_uid",
    token: { email: "edilsontcharlys2010@gmail.com", email_verified: true },
  });

  await assertSucceeds(bobDb.collection("users").doc(PROVIDER_BOB_UID).collection("pixCharges").get());
  await assertSucceeds(adminDb.collection("users").doc(PROVIDER_BOB_UID).collection("pixCharges").get());
  await assertFails(aliceDb.collection("users").doc(PROVIDER_BOB_UID).collection("pixCharges").get());

  // Owner can mark charge CANCELADO_OU_REJEITADO
  await assertSucceeds(
    bobDb.collection("users").doc(PROVIDER_BOB_UID).collection("pixCharges").doc("pix_list_1").update({
      status: "CANCELADO_OU_REJEITADO",
    })
  );
});

test("Login Directory: zero-trust SHA-256 hash get allowed, list strictly denied, owner/admin write only, no PII", async () => {
  const unauthDb = getDb(null);
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const bobDb = getDb({ uid: PROVIDER_BOB_UID });
  const adminDb = getDb({
    uid: "admin_dir_uid",
    token: { email: "edilsontcharlys2010@gmail.com", email_verified: true },
  });

  const validEmailKey = "email_" + "a".repeat(64);
  const validPhoneKey = "phone_" + "b".repeat(64);
  const invalidKey = "alice@example.com";

  // Unauthenticated cannot create or write loginDirectory
  await assertFails(
    unauthDb.collection("loginDirectory").doc(validEmailKey).set({
      uid: CLIENT_ALICE_UID,
      authEmail: "alice@example.com",
      updatedAt: new Date(),
    })
  );

  // Alice CAN create her own valid loginDirectory entries (email + phone hash)
  await assertSucceeds(
    aliceDb.collection("loginDirectory").doc(validEmailKey).set({
      uid: CLIENT_ALICE_UID,
      authEmail: "alice@example.com",
      passwordVerifier: "c".repeat(64),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("loginDirectory").doc(validPhoneKey).set({
      uid: CLIENT_ALICE_UID,
      authEmail: "alice@example.com",
      updatedAt: new Date(),
    })
  );

  // Unknown/PII fields (e.g. phone, name, raw password) MUST be rejected
  await assertFails(
    aliceDb.collection("loginDirectory").doc(validEmailKey).set({
      uid: CLIENT_ALICE_UID,
      authEmail: "alice@example.com",
      phone: "(11) 99999-0001",
      updatedAt: new Date(),
    })
  );

  // Invalid non-SHA256 doc ID MUST be rejected for both write and get
  await assertFails(
    aliceDb.collection("loginDirectory").doc(invalidKey).set({
      uid: CLIENT_ALICE_UID,
      authEmail: "alice@example.com",
      updatedAt: new Date(),
    })
  );
  await assertFails(unauthDb.collection("loginDirectory").doc(invalidKey).get());

  // Unauthenticated CAN get by exact valid SHA-256 lookup key (cross-device pre-login lookup)
  await assertSucceeds(unauthDb.collection("loginDirectory").doc(validEmailKey).get());
  await assertSucceeds(unauthDb.collection("loginDirectory").doc(validPhoneKey).get());

  // List/enumeration is STRICTLY denied for everyone (unauth, user, and even Admin)
  await assertFails(unauthDb.collection("loginDirectory").get());
  await assertFails(aliceDb.collection("loginDirectory").get());
  await assertFails(adminDb.collection("loginDirectory").get());

  // Cross-user Bob CANNOT overwrite or delete Alice's loginDirectory entry
  await assertFails(
    bobDb.collection("loginDirectory").doc(validEmailKey).set({
      uid: PROVIDER_BOB_UID,
      authEmail: "bob@example.com",
      updatedAt: new Date(),
    })
  );
  await assertFails(bobDb.collection("loginDirectory").doc(validEmailKey).delete());

  // Admin CAN update Alice's loginDirectory entry with an aliasAuthEmail after password reset
  await assertSucceeds(
    adminDb.collection("loginDirectory").doc(validEmailKey).set({
      uid: CLIENT_ALICE_UID,
      authEmail: "alice@example.com",
      aliasAuthEmail: "cred_alice_1700000000@auth.resolveai.app",
      passwordVerifier: "d".repeat(64),
      updatedAt: new Date(),
    })
  );
});

test("Linked Auth UIDs: only Admin can link alias UID; linked alias UID can access own profile & subcollections without escalating privileges", async () => {
  const ALIAS_ALICE_UID = "alias_alice_uid_2";
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const aliasDb = getDb({ uid: ALIAS_ALICE_UID });
  const charlieDb = getDb({ uid: CLIENT_CHARLIE_UID });
  const adminDb = getDb({
    uid: "admin_link_uid",
    token: { email: "edilsontcharlys2010@gmail.com", email_verified: true },
  });

  // Regular user CANNOT set linkedAuthUids on profile creation
  await assertFails(
    aliceDb.collection("users").doc(CLIENT_ALICE_UID).set({
      userId: CLIENT_ALICE_UID,
      name: "Alice Silva",
      phone: "+5511999990001",
      role: "CLIENTE",
      linkedAuthUids: [ALIAS_ALICE_UID],
      createdAt: new Date(),
    })
  );

  // Alice creates valid profile without linkedAuthUids
  await assertSucceeds(
    aliceDb.collection("users").doc(CLIENT_ALICE_UID).set({
      userId: CLIENT_ALICE_UID,
      name: "Alice Silva",
      phone: "+5511999990001",
      role: "CLIENTE",
      createdAt: new Date(),
    })
  );

  // Alice CANNOT self-modify linkedAuthUids
  await assertFails(
    aliceDb.collection("users").doc(CLIENT_ALICE_UID).update({
      linkedAuthUids: [ALIAS_ALICE_UID],
    })
  );

  // Before linking, alias UID cannot read Alice's profile
  await assertFails(aliasDb.collection("users").doc(CLIENT_ALICE_UID).get());

  // Verified Admin CAN link alias UID to Alice's profile
  await assertSucceeds(
    adminDb.collection("users").doc(CLIENT_ALICE_UID).update({
      linkedAuthUids: [ALIAS_ALICE_UID],
    })
  );

  // Once linked, alias UID CAN read and update safe profile fields on Alice's doc
  await assertSucceeds(aliasDb.collection("users").doc(CLIENT_ALICE_UID).get());
  await assertSucceeds(
    aliasDb.collection("users").doc(CLIENT_ALICE_UID).update({
      city: "Brasília, DF",
    })
  );

  // Linked alias UID STILL CANNOT escalate role or tamper with linkedAuthUids
  await assertFails(
    aliasDb.collection("users").doc(CLIENT_ALICE_UID).update({
      role: "ADMIN",
    })
  );
  await assertFails(
    aliasDb.collection("users").doc(CLIENT_ALICE_UID).update({
      linkedAuthUids: [ALIAS_ALICE_UID, CLIENT_CHARLIE_UID],
    })
  );

  // Linked alias UID CAN read Alice's notifications, while Charlie still cannot
  await assertSucceeds(aliasDb.collection("users").doc(CLIENT_ALICE_UID).collection("notifications").get());
  await assertFails(charlieDb.collection("users").doc(CLIENT_ALICE_UID).collection("notifications").get());
});

test("RegionalPriceBenchmarks: signed-in users can read/write valid daily benchmarks, invalid payloads & unauth denied", async () => {
  const unauthDb = getDb(null);
  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const adminDb = getDb({
    uid: "admin_uid",
    token: { email: "edilsontcharlys2010@gmail.com", email_verified: true },
  });

  const docId = "20261120__sp_sao_paulo__eletricista";
  const validPayload = {
    id: docId,
    dateKey: "2026-11-20",
    displayDate: "20/11/2026",
    category: "Eletricista",
    regionKey: "sp_sao_paulo",
    regionLabel: "São Paulo - SP",
    stateCode: "SP",
    macroRegion: "Sudeste",
    averagePrice: 295,
    minPrice: 225,
    maxPrice: 385,
    regionalFactor: 1.23,
    localSamplesCount: 3,
    sourceLabel: "Índice regional diário + 3 pedido(s) na região",
    updatedAt: new Date(Date.now() - 1000),
  };

  // Unauthenticated cannot read or write
  await assertFails(unauthDb.collection("regionalPriceBenchmarks").doc(docId).get());
  await assertFails(unauthDb.collection("regionalPriceBenchmarks").doc(docId).set(validPayload));

  // Signed-in user can create and read a valid daily benchmark
  await assertSucceeds(aliceDb.collection("regionalPriceBenchmarks").doc(docId).set(validPayload));
  await assertSucceeds(aliceDb.collection("regionalPriceBenchmarks").doc(docId).get());
  await assertSucceeds(aliceDb.collection("regionalPriceBenchmarks").where("dateKey", "==", "2026-11-20").get());

  // Invalid dateKey or negative/inverted prices are rejected
  await assertFails(
    aliceDb.collection("regionalPriceBenchmarks").doc(docId).set({
      ...validPayload,
      dateKey: "20-11-2026",
    })
  );
  await assertFails(
    aliceDb.collection("regionalPriceBenchmarks").doc(docId).set({
      ...validPayload,
      averagePrice: -10,
    })
  );
  await assertFails(
    aliceDb.collection("regionalPriceBenchmarks").doc(docId).set({
      ...validPayload,
      minPrice: 500,
    })
  );

  // Regular user cannot delete; admin can delete
  await assertFails(aliceDb.collection("regionalPriceBenchmarks").doc(docId).delete());
  await assertSucceeds(adminDb.collection("regionalPriceBenchmarks").doc(docId).delete());
});

test("Client & linked cross-device UID can list ALL provider proposals for their service by serviceId while unrelated user is denied", async () => {
  const ALIAS_ALICE_UID = "alice_device2_uid";
  const ALIAS_BOB_UID = "bob_device2_uid";
  const DAVE_UID = "provider_dave_multi_uid";

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminBypass = ctx.firestore();
    await adminBypass.collection("users").doc(CLIENT_ALICE_UID).set({
      userId: CLIENT_ALICE_UID,
      name: "Alice Cliente",
      phone: "11999990001",
      role: "CLIENTE",
      linkedAuthUids: [ALIAS_ALICE_UID],
      createdAt: new Date(Date.now() - 5000),
    });
    await adminBypass.collection("users").doc(PROVIDER_BOB_UID).set({
      userId: PROVIDER_BOB_UID,
      name: "Bob Prestador",
      phone: "11999990002",
      role: "PRESTADOR",
      linkedAuthUids: [ALIAS_BOB_UID],
      createdAt: new Date(Date.now() - 5000),
    });
    await adminBypass.collection("services").doc("srv_multi_props").set({
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Cliente",
      title: "Instalação elétrica completa",
      category: "Eletricista",
      proposedBudget: 350,
      status: "ABERTO",
      createdAt: new Date(Date.now() - 4000),
    });
  });

  const aliceDb = getDb({ uid: CLIENT_ALICE_UID });
  const aliceDevice2Db = getDb({ uid: ALIAS_ALICE_UID });
  const bobDevice2Db = getDb({ uid: ALIAS_BOB_UID });
  const daveDb = getDb({ uid: DAVE_UID });
  const charlieDb = getDb({ uid: CLIENT_CHARLIE_UID });
  const assertStrict = require("node:assert/strict");

  // Provider Bob (from linked second device) creates proposal with long multi-specialty category
  await assertSucceeds(
    bobDevice2Db.collection("proposals").doc("prop_multi_bob").set({
      serviceId: "srv_multi_props",
      clientId: CLIENT_ALICE_UID,
      providerId: PROVIDER_BOB_UID,
      providerName: "Bob Multi",
      providerCategory: "Eletricista, Hidráulica, Pintura, Montagem de Móveis, TI, Redes & Informática, Ar Condicionado, Manutenção Predial",
      proposedPrice: 320,
      estimatedDays: 1,
      description: "Faço hoje mesmo",
      status: "ENVIADA",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Provider Dave creates a second proposal even if clientId had a legacy/local ID mismatch
  await assertSucceeds(
    daveDb.collection("proposals").doc("prop_multi_dave").set({
      serviceId: "srv_multi_props",
      clientId: "cli_legacy_local_id",
      providerId: DAVE_UID,
      providerName: "Dave Eletricista",
      providerCategory: "Eletricista",
      proposedPrice: 340,
      estimatedDays: 1,
      description: "Disponível amanhã",
      status: "ENVIADA",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Provider Dave updates existing proposal to direct acceptance without modifying createdAt
  await assertSucceeds(
    daveDb.collection("proposals").doc("prop_multi_dave").update({
      proposedPrice: 350,
      isDirectAcceptance: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Primary client Alice queries by serviceId and gets ALL 2 proposals (even the one with legacy clientId!)
  const aliceSnap = await assertSucceeds(
    aliceDb.collection("proposals").where("serviceId", "==", "srv_multi_props").get()
  );
  assertStrict.equal(aliceSnap.size, 2);

  // Linked second-device client Alice ALSO gets ALL 2 proposals by serviceId AND by primary clientId!
  const aliceDevice2ByService = await assertSucceeds(
    aliceDevice2Db.collection("proposals").where("serviceId", "==", "srv_multi_props").get()
  );
  assertStrict.equal(aliceDevice2ByService.size, 2);

  const aliceDevice2ByClient = await assertSucceeds(
    aliceDevice2Db.collection("proposals").where("clientId", "==", CLIENT_ALICE_UID).get()
  );
  assertStrict.equal(aliceDevice2ByClient.size, 1);

  // Unrelated user Charlie is strictly denied from listing proposals for Alice's service
  await assertFails(
    charlieDb.collection("proposals").where("serviceId", "==", "srv_multi_props").get()
  );
});

test("Chat delivery works for primary and linked multi-device client & provider, while denying unrelated users and open services", async () => {
  const ALIAS_ALICE_CHAT_UID = "alice_chat_dev2";
  const ALIAS_BOB_CHAT_UID = "bob_chat_dev2";
  const assertStrict = require("node:assert/strict");

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminBypass = ctx.firestore(DATABASE_ID);
    await adminBypass.collection("users").doc(CLIENT_ALICE_UID).set({
      userId: CLIENT_ALICE_UID,
      name: "Alice Cliente",
      phone: "11999991001",
      role: "CLIENTE",
      linkedAuthUids: [ALIAS_ALICE_CHAT_UID],
      createdAt: new Date(Date.now() - 5000),
    });
    await adminBypass.collection("users").doc(PROVIDER_BOB_UID).set({
      userId: PROVIDER_BOB_UID,
      name: "Bob Prestador",
      phone: "11999991002",
      role: "PRESTADOR",
      linkedAuthUids: [ALIAS_BOB_CHAT_UID],
      createdAt: new Date(Date.now() - 5000),
    });
    await adminBypass.collection("services").doc("srv_chat_open").set({
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Cliente",
      title: "Serviço ainda aberto",
      category: "Eletricista",
      proposedBudget: 200,
      status: "ABERTO",
      createdAt: new Date(Date.now() - 4000),
    });
    await adminBypass.collection("services").doc("srv_chat_active").set({
      clientId: CLIENT_ALICE_UID,
      clientName: "Alice Cliente",
      title: "Serviço em andamento",
      category: "Eletricista",
      proposedBudget: 350,
      status: "EM_EXECUCAO",
      assignedProviderId: PROVIDER_BOB_UID,
      acceptedProviderId: PROVIDER_BOB_UID,
      acceptedProviderName: "Bob Prestador",
      createdAt: new Date(Date.now() - 4000),
    });
  });

  const aliceDevice2Db = getDb({ uid: ALIAS_ALICE_CHAT_UID });
  const bobDevice2Db = getDb({ uid: ALIAS_BOB_CHAT_UID });
  const charlieDb = getDb({ uid: CLIENT_CHARLIE_UID });

  // Chat blocked while service is still ABERTO
  await assertFails(
    aliceDevice2Db.collection("services").doc("srv_chat_open").collection("messages").doc("msg_open_1").set({
      serviceId: "srv_chat_open",
      senderId: ALIAS_ALICE_CHAT_UID,
      senderName: "Alice Cliente",
      senderRole: "CLIENTE",
      content: "Olá",
      isSystemNotice: false,
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Linked second-device client CAN send and read chat messages
  await assertSucceeds(
    aliceDevice2Db.collection("services").doc("srv_chat_active").collection("messages").doc("msg_cli_dev2").set({
      serviceId: "srv_chat_active",
      senderId: ALIAS_ALICE_CHAT_UID,
      senderName: "Alice Cliente",
      senderRole: "CLIENTE",
      content: "Pode vir às 14h?",
      isSystemNotice: false,
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Linked second-device provider CAN send (using either device UID or canonical profile UID) and list messages
  await assertSucceeds(
    bobDevice2Db.collection("services").doc("srv_chat_active").collection("messages").doc("msg_pro_dev2").set({
      serviceId: "srv_chat_active",
      senderId: PROVIDER_BOB_UID,
      senderName: "Bob Prestador",
      senderRole: "PRESTADOR",
      content: "Combinado, estarei aí às 14h!",
      isSystemNotice: false,
      createdAt: new Date(Date.now() - 500),
    })
  );

  const cliMsgs = await assertSucceeds(
    aliceDevice2Db.collection("services").doc("srv_chat_active").collection("messages").orderBy("createdAt", "asc").get()
  );
  assertStrict.equal(cliMsgs.size, 2);

  const proMsgs = await assertSucceeds(
    bobDevice2Db.collection("services").doc("srv_chat_active").collection("messages").orderBy("createdAt", "asc").get()
  );
  assertStrict.equal(proMsgs.size, 2);

  // Unrelated user Charlie is strictly denied from reading or sending messages
  await assertFails(
    charlieDb.collection("services").doc("srv_chat_active").collection("messages").get()
  );
  await assertFails(
    charlieDb.collection("services").doc("srv_chat_active").collection("messages").doc("msg_charlie").set({
      serviceId: "srv_chat_active",
      senderId: CLIENT_CHARLIE_UID,
      senderName: "Charlie",
      senderRole: "CLIENTE",
      content: "Intruso",
      isSystemNotice: false,
      createdAt: new Date(Date.now() - 500),
    })
  );
});



