import type { OnInit } from '@angular/core';
import {Component, inject, signal} from '@angular/core';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonList,
  IonItem,
  IonText,
  IonButton,
  IonLabel, Platform,
} from '@ionic/angular/standalone';
import { CodeScanner } from '@rdlabo/capacitor-codescanner';

@Component({
  selector: 'app-tab1',
  templateUrl: 'tab1.page.html',
  styleUrls: ['tab1.page.scss'],
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonList, IonItem, IonButton, IonLabel],
})
export class Tab1Page implements OnInit {
  platform = inject(Platform)
  codes = signal<string[]>([]);

  ngOnInit(): void {
    CodeScanner.addListener('CodeScannerCatchEvent', (info) => {
      this.codes.update((codes) => [info.code, ...codes]);
    });
  }

  async present(): Promise<void> {
    await CodeScanner.present({
      isMulti: false,
      enableAutoLight: false,
      enableCloseButton: this.platform.is('android'),
      sheetScreenRatio: 0.3,
    });
  }
}
