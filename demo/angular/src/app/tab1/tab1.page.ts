import type { OnInit } from '@angular/core';
import { Component, signal } from '@angular/core';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonList,
  IonItem,
  IonText,
  IonButton,
  IonLabel,
} from '@ionic/angular/standalone';
import { CodeScanner } from '@rdlabo/capacitor-codescanner';

@Component({
  selector: 'app-tab1',
  templateUrl: 'tab1.page.html',
  styleUrls: ['tab1.page.scss'],
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonList, IonItem, IonText, IonButton, IonLabel],
})
export class Tab1Page implements OnInit {
  codes = signal<string[]>([]);

  ngOnInit(): void {
    CodeScanner.addListener('CodeScannerCatchEvent', (info) => {
      this.codes.update((codes) => [info.code, ...codes]);
    });
  }

  async present(): Promise<void> {
    await CodeScanner.present({});
  }
}
