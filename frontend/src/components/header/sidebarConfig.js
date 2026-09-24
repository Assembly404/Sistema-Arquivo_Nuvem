import {
    FiBell,
    FiFile,
    FiFolder,
    FiHardDrive,
    FiHelpCircle,
    FiHome,
    FiKey,
    FiLogOut,
    FiSettings,
    FiShare2,
    FiShield,
    FiTrash2,
    FiUser,
    FiUsers
} from "react-icons/fi";

export const SIDEBAR_STORAGE_KEY = "sidebar:collapsed";

export const SIDEBAR_ICONS = {
    FiBell,
    FiFile,
    FiFolder,
    FiHardDrive,
    FiHelpCircle,
    FiHome,
    FiKey,
    FiLogOut,
    FiSettings,
    FiShare2,
    FiShield,
    FiTrash2,
    FiUser,
    FiUsers
};

export const SIDEBAR_MENU = [
    {
        id: "dashboard",
        label: "Início",
        path: "/dashboard",
        icon: "FiHome",
        permission: null,
        visibility: "public",
        badge: null,
        children: []
    },
    {
        id: "arquivos",
        label: "Arquivos",
        path: null,
        icon: "FiFolder",
        permission: "ARQUIVO_VISUALIZAR",
        visibility: "public",
        badge: null,
        children: [
            {
                id: "arquivos-meus",
                label: "Meus Arquivos",
                path: "/dashboard/arquivos",
                icon: "FiFile",
                permission: "ARQUIVO_VISUALIZAR",
                visibility: "public",
                badge: null,
                children: []
            },
            {
                id: "arquivos-compartilhados",
                label: "Compartilhados comigo",
                path: "/dashboard/arquivos/compartilhados",
                icon: "FiShare2",
                permission: "ARQUIVO_VISUALIZAR",
                visibility: "public",
                badge: null,
                children: []
            },
            {
                id: "arquivos-lixeira",
                label: "Lixeira",
                path: "/dashboard/arquivos/lixeira",
                icon: "FiTrash2",
                permission: "ARQUIVO_EXCLUIR",
                visibility: "conditional",
                tooltip: "O seu perfil não permite excluir ficheiros.",
                badge: null,
                children: []
            }
        ]
    },
    {
        id: "armazenamento",
        label: "Armazenamento",
        path: "/dashboard/armazenamento",
        icon: "FiHardDrive",
        permission: "ARMAZENAMENTO_VISUALIZAR",
        visibility: "public",
        badge: null,
        children: []
    },
    {
        id: "admin",
        label: "Administração",
        path: null,
        icon: "FiShield",
        permission: "ADMIN_ACESSAR",
        visibility: "secret",
        badge: null,
        children: [
            {
                id: "admin-usuarios",
                label: "Usuários",
                path: "/dashboard/admin/usuarios",
                icon: "FiUsers",
                permission: "ADMIN_USUARIOS",
                visibility: "secret",
                badge: null,
                children: []
            },
            {
                id: "admin-permissoes",
                label: "Perfis & Permissões",
                path: "/dashboard/admin/permissoes",
                icon: "FiKey",
                permission: "ADMIN_PERMISSOES",
                visibility: "secret",
                badge: null,
                children: []
            }
        ]
    },
    {
        id: "notificacoes",
        label: "Notificações",
        path: "/dashboard/notificacoes",
        icon: "FiBell",
        permission: null,
        visibility: "public",
        badge: null,
        children: []
    },
    {
        id: "suporte",
        label: "Suporte",
        path: "/dashboard/suporte",
        icon: "FiHelpCircle",
        permission: null,
        visibility: "public",
        badge: null,
        children: []
    },
    {
        id: "configuracoes",
        label: "Configurações",
        path: "/dashboard/configuracoes",
        icon: "FiSettings",
        permission: null,
        visibility: "public",
        badge: null,
        children: []
    }
];

export const SIDEBAR_ACCOUNT_MENU = [
    {
        id: "perfil",
        label: "Perfil",
        path: "/dashboard/perfil",
        icon: "FiUser"
    },
    {
        id: "logout",
        label: "Terminar sessão",
        action: "logout",
        icon: "FiLogOut",
        danger: true
    }
];
