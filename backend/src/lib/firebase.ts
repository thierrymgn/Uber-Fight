// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import {getFirestore} from "@firebase/firestore";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
    apiKey: "AIzaSyAZ3qxW_rdZ8fCSxMai5vsNSpI5wdRzDz8",
    authDomain: "uber-fight.firebaseapp.com",
    databaseURL: "https://uber-fight-default-rtdb.europe-west1.firebasedatabase.app",
    projectId: "uber-fight",
    storageBucket: "uber-fight.firebasestorage.app",
    messagingSenderId: "1089591370423",
    appId: "1:1089591370423:web:c73cd7d52f67318be0a332",
    measurementId: "G-5V1Z63ZBZG"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
//export const analytics = getAnalytics(app);